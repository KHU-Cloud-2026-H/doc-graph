"""UC6 — 연결 제안 수락 (P1).

usecases.md UC6 참고. 문서 그래프의 점선 엣지(EdgeProposal)를 수락하면
실선 DependencyEdge로 전환되고 정합성 검증이 즉시 트리거된다.

검증 대상 외 prerequisite(workspace·project·document·proposal)은 acceptance
fixture endpoint로 우회 생성한다 (conftest seeder). 검증 axis는
POST /projects/{id}/proposals/{proposalId}/accept.

현 시점 미구현이므로 모든 단언은 fail이 정상 (spec-first).
구현 진행에 따라 단계별로 통과한다.
"""

SOURCE_PAGE_ID = "66666666-6666-6666-6666-660000000001"
TARGET_PAGE_ID = "66666666-6666-6666-6666-660000000002"


def test_uc6_accept_proposal_converts_to_edge_and_triggers_validation(
    client, wiremock, auth_headers, seeder, wait_until, stub_ai
):
    """UC6 happy path: 점선 proposal 수락 → 실선 edge 전환 → 정합성 검증 트리거."""
    workspace_id = seeder.workspace(notion_workspace_id="test-ws-uc6", name="UC6 Workspace")
    project_id = seeder.project(workspace_id, name="UC6 Project")
    source_doc_id = seeder.document(
        project_id, notion_page_id=SOURCE_PAGE_ID, title="UC6 Source",
    )
    target_doc_id = seeder.document(
        project_id, notion_page_id=TARGET_PAGE_ID, title="UC6 Target",
    )
    proposal_id = seeder.proposal(
        project_id=project_id,
        source_document_id=source_doc_id,
        target_document_id=target_doc_id,
    )

    # 정합성 검증 시 AI 응답 (충돌 없음)
    stub_ai()

    # 검증 axis: 제안 수락
    accept = client.post(
        f"/projects/{project_id}/proposals/{proposal_id}/accept",
        headers=auth_headers,
    )
    assert accept.status_code in (200, 201)
    edge_id = accept.json()["id"]

    wait_until(
        lambda: _proposal_removed_and_edge_present(
            client, auth_headers, project_id, proposal_id, edge_id
        ),
        reason="proposal이 그래프에서 제거되거나 새 edge가 등장하지 않음",
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


def _proposal_removed_and_edge_present(client, headers, project_id, proposal_id, edge_id) -> bool:
    response = client.get(f"/projects/{project_id}/graph", headers=headers)
    if response.status_code != 200:
        return False
    body = response.json()
    proposal_gone = not any(p.get("id") == proposal_id for p in body.get("proposals", []))
    edge_present = any(e.get("id") == edge_id for e in body.get("edges", []))
    return proposal_gone and edge_present