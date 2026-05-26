"""UC2: 프로젝트 생성 및 초기 동기화.

워크스페이스가 등록된 상태에서 프로젝트를 생성하고 Notion 루트 페이지
하위 트리를 동기화한다. 동기화 후 그래프에 문서 노드가 보여야 한다.

워크스페이스 등록은 acceptance scope 외라 `seeder.workspace` fixture로 우회.
"""

import time


def _stub_notion_page(wiremock, page_id: str, title: str, children: list[dict]):
    wiremock.stub(
        request={"method": "GET", "urlPathPattern": f"/v1/pages/{page_id}"},
        response={
            "status": 200,
            "headers": {"Content-Type": "application/json"},
            "jsonBody": {
                "object": "page",
                "id": page_id,
                "properties": {
                    "title": {"title": [{"plain_text": title}]},
                },
            },
        },
    )
    wiremock.stub(
        request={"method": "GET", "urlPathPattern": f"/v1/blocks/{page_id}/children"},
        response={
            "status": 200,
            "headers": {"Content-Type": "application/json"},
            "jsonBody": {
                "object": "list",
                "results": children,
                "has_more": False,
                "next_cursor": None,
            },
        },
    )


def _wait_for(check, timeout: int = 60, interval: int = 2):
    deadline = time.time() + timeout
    while time.time() < deadline:
        result = check()
        if result is not None:
            return result
        time.sleep(interval)
    raise TimeoutError("condition not met within timeout")


def test_uc2_create_project_and_sync(client, wiremock, auth_headers, seeder):
    """프로젝트 생성 → Notion 초기 동기화 → 문서 저장 → edge 생성 → validation enqueue."""
    workspace_id = seeder.workspace(notion_workspace_id="test-ws-uc2", name="UC2 Workspace")

    # Notion API stub: 루트 페이지 + 자식 2개 (planning, requirements)
    _stub_notion_page(
        wiremock,
        page_id="root_page_uc2",
        title="UC2 Root",
        children=[
            {
                "object": "block",
                "type": "child_page",
                "id": "planning_page",
                "child_page": {"title": "Planning"},
            },
            {
                "object": "block",
                "type": "child_page",
                "id": "requirements_page",
                "child_page": {"title": "Requirements"},
            },
        ],
    )
    _stub_notion_page(
        wiremock,
        page_id="planning_page",
        title="Planning",
        children=[
            {
                "object": "block",
                "type": "paragraph",
                "id": "planning_block_1",
                "has_children": False,
                "paragraph": {
                    "rich_text": [
                        {"type": "text", "plain_text": "Requirements must reflect this plan. "},
                        {
                            "type": "mention",
                            "plain_text": "Requirements",
                            "mention": {
                                "type": "page",
                                "page": {"id": "requirements_page"},
                            },
                        },
                    ],
                },
            },
        ],
    )
    _stub_notion_page(
        wiremock,
        page_id="requirements_page",
        title="Requirements",
        children=[
            {
                "object": "block",
                "type": "paragraph",
                "id": "requirements_block_1",
                "has_children": False,
                "paragraph": {
                    "rich_text": [{"type": "text", "plain_text": "Initial requirement text"}],
                },
            },
        ],
    )
    wiremock.stub(
        request={"method": "POST", "urlPattern": "/.*/(chat/completions|messages)"},
        response={"status": 200, "jsonBody": {"conflicts": []}},
    )

    # 1. 프로젝트 생성
    create_project = client.post(
        f"/workspaces/{workspace_id}/projects",
        headers=auth_headers,
        json={
            "name": "UC2 Project",
            "notionRootPageId": "root_page_uc2",
        },
    )
    assert create_project.status_code == 200
    project_id = create_project.json()["id"]

    # 2. 카테고리 등록 (개별 POST N번)
    for notion_page_id, document_type in [
        ("planning_page", "planning"),
        ("requirements_page", "requirements"),
    ]:
        response = client.post(
            f"/projects/{project_id}/categories",
            headers=auth_headers,
            json={"notionPageId": notion_page_id, "documentType": document_type},
        )
        assert response.status_code == 200

    # 3. 동기화 트리거
    sync = client.post(
        f"/projects/{project_id}/sync",
        headers=auth_headers,
    )
    assert sync.status_code in (200, 202, 204)

    # 4. 동기화 완료 → 문서 목록에 root + child pages 저장
    def _documents_synced():
        response = client.get(
            f"/projects/{project_id}/documents?size=20",
            headers=auth_headers,
        )
        if response.status_code != 200:
            return None
        docs = response.json().get("content", [])
        page_ids = {doc.get("notionPageId") for doc in docs}
        if {"root_page_uc2", "planning_page", "requirements_page"}.issubset(page_ids):
            return docs
        return None

    docs = _wait_for(_documents_synced, timeout=60)
    planning_doc = next(doc for doc in docs if doc["notionPageId"] == "planning_page")
    requirements_doc = next(doc for doc in docs if doc["notionPageId"] == "requirements_page")

    detail = client.get(f"/documents/{planning_doc['id']}", headers=auth_headers)
    assert detail.status_code == 200
    assert any(block["blockId"] == "planning_block_1" for block in detail.json()["blocks"])

    # 5. mention 기반 edge 생성 → graph에 표시
    def _graph_has_edge():
        response = client.get(
            f"/projects/{project_id}/graph",
            headers=auth_headers,
        )
        if response.status_code != 200:
            return None
        body = response.json()
        edges = body.get("edges", [])
        edge = next(
            (
                item for item in edges
                if item.get("sourceDocumentId") == planning_doc["id"]
                and item.get("targetDocumentId") == requirements_doc["id"]
                and item.get("source") == "NOTION_REFERENCE"
            ),
            None,
        )
        return {"graph": body, "edge": edge} if edge else None

    graph_result = _wait_for(_graph_has_edge, timeout=60)
    assert len(graph_result["graph"]["nodes"]) >= 3

    # 6. edge 생성이 validation task enqueue까지 연결됨
    edge_id = graph_result["edge"]["id"]

    def _validation_task_created():
        response = client.get(
            f"/projects/{project_id}/validation-tasks",
            headers=auth_headers,
        )
        if response.status_code != 200:
            return None
        tasks = response.json().get("content", [])
        return tasks if any(task.get("edgeId") == edge_id for task in tasks) else None

    _wait_for(_validation_task_created, timeout=60)

    # 7. Notion API가 실제로 호출됐는지 확인
    notion_calls = wiremock.calls()
    called_paths = [
        call.get("request", {}).get("url", "") for call in notion_calls
    ]
    assert any("/v1/pages/root_page_uc2" in path for path in called_paths)
    assert any("/v1/blocks/" in path for path in called_paths)
