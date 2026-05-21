"""UC4 — 충돌 해소: 제안 승인 후 Notion 반영.

usecases.md UC4 참고. 비교 패널에서 AI 수정 제안을 수락하면 Notion API로
변경이 반영되고, 재검증 후 Conflict가 비활성화되는 흐름을 검증한다.

검증 대상 외 prerequisite(workspace·project·document·edge·conflict·finding)은
acceptance fixture endpoint로 우회 생성한다 (conftest seeder). 검증 axis는
POST /conflicts/{conflictId}/findings/{findingId}/approve.

현 시점 미구현이므로 모든 단언은 fail이 정상 (spec-first).
구현 진행에 따라 단계별로 통과한다.
"""

SOURCE_PAGE_ID = "44444444-4444-4444-4444-440000000001"
TARGET_PAGE_ID = "44444444-4444-4444-4444-440000000002"
SOURCE_BLOCK_ID = "uc4-source-block"
TARGET_BLOCK_ID = "uc4-target-block"
SUGGESTION_TEXT = "출시일을 2주 미룬다 (요구사항 문서 반영)"


def test_uc4_approve_finding_reflects_to_notion_and_resolves_conflict(
    client, wiremock, auth_headers, seeder, inbox_has, wait_until
):
    """UC4 happy path: 제안 승인 → Notion PATCH → 재검증 → Conflict 비활성화."""
    workspace_id = seeder.workspace(notion_workspace_id="test-ws-uc4", name="UC4 Workspace")
    project_id = seeder.project(workspace_id, name="UC4 Project")
    source_doc_id = seeder.document(
        project_id, notion_page_id=SOURCE_PAGE_ID, title="UC4 Source",
        block_ids=[SOURCE_BLOCK_ID],
    )
    target_doc_id = seeder.document(
        project_id, notion_page_id=TARGET_PAGE_ID, title="UC4 Target",
        block_ids=[TARGET_BLOCK_ID],
    )
    edge_id = seeder.edge(source_document_id=source_doc_id, target_document_id=target_doc_id)
    conflict = seeder.conflict(
        edge_id=edge_id,
        findings=[
            {
                "sourceBlockIds": [SOURCE_BLOCK_ID],
                "targetBlockIds": [TARGET_BLOCK_ID],
                "rationale": "UC4 출시일 불일치",
                "suggestion": SUGGESTION_TEXT,
            }
        ],
    )
    conflict_id = conflict["id"]
    finding_id = conflict["findings"][0]["id"]

    # Notion PATCH stub (제안 반영) + 재검증 시 AI 응답(충돌 없음)
    wiremock.stub(
        request={"method": "PATCH", "urlPattern": f"/v1/blocks/{TARGET_BLOCK_ID}"},
        response={"status": 200, "jsonBody": {"object": "block", "id": TARGET_BLOCK_ID}},
    )
    wiremock.stub(
        request={"method": "POST", "urlPattern": "/.*/(chat/completions|messages)"},
        response={"status": 200, "jsonBody": {"conflicts": []}},
    )

    # 검증 axis: 제안 승인
    approve = client.post(
        f"/conflicts/{conflict_id}/findings/{finding_id}/approve",
        headers=auth_headers,
    )
    assert approve.status_code in (200, 202, 204)

    wait_until(
        lambda: any(
            call.get("request", {}).get("method") == "PATCH"
            and TARGET_BLOCK_ID in (call.get("request", {}).get("url") or "")
            for call in wiremock.calls()
        ),
        reason="Notion API에 PATCH 호출이 발생하지 않음",
    )

    wait_until(
        lambda: not inbox_has(conflict_id, headers=auth_headers),
        reason="Conflict가 인박스에서 제거되지 않음",
    )