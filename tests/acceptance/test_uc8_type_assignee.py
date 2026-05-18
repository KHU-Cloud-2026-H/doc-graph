"""UC8 — 담당자 설정 (P1).

usecases.md UC8 참고. Project Admin이 타입별 담당자 기본값을 설정하면 이후
해당 타입 문서가 target인 충돌이 지정 담당자의 인박스로 라우팅된다.

검증 대상 외 prerequisite(workspace·project·assignee 멤버·문서·edge·conflict)은
acceptance fixture endpoint로 우회 생성한다 (conftest seeder). 검증 axis는
PUT /projects/{id}/type-assignees.

현 시점 미구현이므로 모든 단언은 fail이 정상 (spec-first).
구현 진행에 따라 단계별로 통과한다.
"""

SOURCE_PAGE_ID = "88888888-8888-8888-8888-880000000001"
TARGET_PAGE_ID = "88888888-8888-8888-8888-880000000002"
TARGET_TYPE = "requirements"


def test_uc8_type_assignee_routes_conflict_to_inbox(
    client, auth_headers, seeder, inbox_has, wait_until
):
    """UC8 happy path: 담당자 매핑 설정 후 해당 타입 conflict가 담당자 인박스로 라우팅."""
    workspace_id = seeder.workspace(notion_workspace_id="test-ws-uc8", name="UC8 Workspace")
    project_id = seeder.project(workspace_id, name="UC8 Project")
    assignee_member_id = seeder.workspace_member(workspace_id=workspace_id)
    seeder.project_member(project_id=project_id, member_id=assignee_member_id)

    # 검증 axis: 타입별 담당자 매핑 설정
    response = client.put(
        f"/projects/{project_id}/type-assignees",
        headers=auth_headers,
        json={
            "assignees": [
                {"documentType": TARGET_TYPE, "assigneeMemberId": assignee_member_id}
            ]
        },
    )
    assert response.status_code in (200, 204)

    # 사후 검증: 해당 타입이 target인 충돌이 발생하면 담당자 인박스에 등장
    source_doc_id = seeder.document(
        project_id, notion_page_id=SOURCE_PAGE_ID, title="UC8 Source",
        document_type="planning",
    )
    target_doc_id = seeder.document(
        project_id, notion_page_id=TARGET_PAGE_ID, title="UC8 Target",
        document_type=TARGET_TYPE,
    )
    edge_id = seeder.edge(source_document_id=source_doc_id, target_document_id=target_doc_id)
    conflict = seeder.conflict(
        edge_id=edge_id,
        findings=[
            {
                "sourceBlockIds": ["uc8-source-block"],
                "targetBlockIds": ["uc8-target-block"],
                "rationale": "UC8 라우팅 검증용 충돌",
            }
        ],
    )
    conflict_id = conflict["id"]

    assignee_headers = {"X-Test-User-Id": str(assignee_member_id)}
    wait_until(
        lambda: inbox_has(conflict_id, headers=assignee_headers),
        reason="설정된 담당자 인박스에 conflict가 라우팅되지 않음",
    )