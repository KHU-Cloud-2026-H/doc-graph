"""UC7 — 수동 엣지 추가 (P2).

usecases.md UC7 참고. Admin이 출발·도착 문서 + 검증 기준을 직접 입력해 엣지를
생성하면 정합성 검증이 즉시 트리거된다.

검증 대상 외 prerequisite(workspace·project·document)은 acceptance fixture
endpoint로 우회 생성한다 (conftest seeder). 검증 axis는 POST /projects/{id}/edges.
"""

SOURCE_PAGE_ID = "77777777-7777-7777-7777-770000000001"
TARGET_PAGE_ID = "77777777-7777-7777-7777-770000000002"


def test_uc7_admin_adds_custom_edge_and_triggers_validation(
    client, wiremock, auth_headers, seeder, wait_until
):
    """UC7 happy path: 직접 edge 생성 → graph에 등장 → 정합성 검증 트리거."""
    workspace_id = seeder.workspace(notion_workspace_id="test-ws-uc7", name="UC7 Workspace")
    project_id = seeder.project(workspace_id, name="UC7 Project")
    source_doc_id = seeder.document(
        project_id, notion_page_id=SOURCE_PAGE_ID, title="UC7 Source",
    )
    target_doc_id = seeder.document(
        project_id, notion_page_id=TARGET_PAGE_ID, title="UC7 Target",
    )

    wiremock.stub(
        request={"method": "POST", "urlPattern": "/.*/(chat/completions|messages)"},
        response={"status": 200, "jsonBody": {"conflicts": []}},
    )

    create = client.post(
        f"/projects/{project_id}/edges",
        headers=auth_headers,
        json={
            "sourceDocumentId": source_doc_id,
            "targetDocumentId": target_doc_id,
            "validationCriterion": "범위 일치 여부",
        },
    )
    assert create.status_code in (200, 201)
    edge_id = create.json()["id"]

    wait_until(
        lambda: _edge_present(client, auth_headers, project_id, edge_id),
        reason="새 edge가 graph에 등장하지 않음",
    )

    wait_until(
        lambda: any(
            call.get("request", {}).get("method") == "POST"
            and any(
                key in (call.get("request", {}).get("url") or "")
                for key in ("chat/completions", "messages")
            )
            for call in wiremock.calls()
        ),
        reason="정합성 검증을 위한 AI API 호출이 발생하지 않음",
    )


def _edge_present(client, headers, project_id, edge_id) -> bool:
    response = client.get(f"/projects/{project_id}/graph", headers=headers)
    if response.status_code != 200:
        return False
    body = response.json()
    return any(e.get("id") == edge_id for e in body.get("edges", []))
