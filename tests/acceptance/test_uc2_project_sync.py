"""UC2: 프로젝트 생성 및 초기 동기화.

워크스페이스가 등록된 상태에서 프로젝트를 생성하고 Notion 루트 페이지
하위 트리를 동기화한다. 동기화 후 그래프에 문서 노드가 보여야 한다.
"""

import time


def _create_workspace(client, headers, notion_id: str = "test-ws-uc2"):
    response = client.post(
        "/workspaces",
        headers=headers,
        json={"notionWorkspaceId": notion_id, "name": "UC2 Workspace"},
    )
    assert response.status_code == 201
    return response.json()["id"]


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


def test_uc2_create_project_and_sync(client, wiremock, auth_headers):
    """프로젝트 생성 → 타입 매핑 → 동기화 → 그래프에 노드 표시."""
    workspace_id = _create_workspace(client, auth_headers)

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
    # 자식 페이지의 children은 비어 있음 (depth 1로 충분)
    _stub_notion_page(wiremock, page_id="planning_page", title="Planning", children=[])
    _stub_notion_page(wiremock, page_id="requirements_page", title="Requirements", children=[])

    # 1. 프로젝트 생성
    create_project = client.post(
        f"/workspaces/{workspace_id}/projects",
        headers=auth_headers,
        json={
            "name": "UC2 Project",
            "rootPageId": "root_page_uc2",
        },
    )
    assert create_project.status_code == 201
    project_id = create_project.json()["id"]

    # 2. 타입 매핑 설정
    mapping = client.post(
        f"/projects/{project_id}/type-mappings",
        headers=auth_headers,
        json={
            "mappings": [
                {"parentPageId": "planning_page", "type": "planning"},
                {"parentPageId": "requirements_page", "type": "requirements"},
            ],
        },
    )
    assert mapping.status_code in (200, 204)

    # 3. 동기화 트리거
    sync = client.post(
        f"/projects/{project_id}/sync",
        headers=auth_headers,
    )
    assert sync.status_code in (200, 202)

    # 4. 동기화 완료 → 그래프에 문서 노드 표시
    def _graph_has_nodes():
        response = client.get(
            f"/projects/{project_id}/graph",
            headers=auth_headers,
        )
        if response.status_code != 200:
            return None
        nodes = response.json().get("nodes", [])
        return nodes if nodes else None

    nodes = _wait_for(_graph_has_nodes, timeout=60)
    # 매핑된 두 타입의 문서 노드가 등록됨
    assert len(nodes) >= 2

    # 5. Notion API가 실제로 호출됐는지 확인
    notion_calls = wiremock.calls()
    called_paths = [
        call.get("request", {}).get("url", "") for call in notion_calls
    ]
    assert any("/v1/pages/root_page_uc2" in path for path in called_paths)
    assert any("/v1/blocks/" in path for path in called_paths)