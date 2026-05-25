"""UC5 — 인박스에서 충돌 확인 및 무시 처리 (P1).

usecases.md UC5 참고. 인박스의 미해소 충돌을 "무시"로 마킹하면 인박스에서
사라지고 ignoredAt·ignoredBy가 기록되는 흐름을 검증한다.

검증 대상 외 prerequisite(workspace·project·document·edge·conflict)은
acceptance fixture endpoint로 우회 생성한다 (conftest seeder). 검증 axis는
POST /conflicts/{conflictId}/ignore.

현 시점 미구현이므로 모든 단언은 fail이 정상 (spec-first).
구현 진행에 따라 단계별로 통과한다.
"""

SOURCE_PAGE_ID = "55555555-5555-5555-5555-550000000001"
TARGET_PAGE_ID = "55555555-5555-5555-5555-550000000002"


def test_uc5_ignore_conflict_removes_from_inbox(
    client, auth_headers, seeder, inbox_has, wait_until
):
    """UC5 happy path: 인박스 노출 → 무시 마킹 → 인박스에서 제거."""
    workspace_id = seeder.workspace(notion_workspace_id="test-ws-uc5", name="UC5 Workspace")
    project_id = seeder.project(workspace_id, name="UC5 Project")
    source_doc_id = seeder.document(
        project_id, notion_page_id=SOURCE_PAGE_ID, title="UC5 Source",
    )
    target_doc_id = seeder.document(
        project_id, notion_page_id=TARGET_PAGE_ID, title="UC5 Target",
    )
    edge_id = seeder.edge(
        project_id=project_id,
        source_document_id=source_doc_id,
        target_document_id=target_doc_id,
    )
    conflict = seeder.conflict(
        edge_id=edge_id,
        findings=[
            {
                "sourceBlockIds": ["uc5-source-block"],
                "targetBlockId": "uc5-target-block",
                "rationale": "UC5 의미적 불일치",
            }
        ],
    )
    conflict_id = conflict["id"]

    assert inbox_has(conflict_id, headers=auth_headers), (
        "fixture seed 직후 conflict가 인박스에 노출되어 있어야 함"
    )

    # 검증 axis: 무시 마킹
    ignore = client.post(
        f"/conflicts/{conflict_id}/ignore",
        headers=auth_headers,
        json={"reason": "의도된 차이로 확인됨"},
    )
    assert ignore.status_code in (200, 204)

    wait_until(
        lambda: not inbox_has(conflict_id, headers=auth_headers),
        reason="무시 마킹 후에도 conflict가 인박스에 남아 있음",
    )