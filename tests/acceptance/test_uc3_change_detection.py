"""UC3 — 문서 변경 감지 및 충돌 알림 수신.

usecases.md UC3 참고. Notion webhook 수신 → 본문 조회 → AI 정합성 검증 →
충돌 감지·인박스 노출의 end-to-end 흐름을 검증한다.

현 시점 백엔드 미구현이므로 모든 단언은 fail이 정상 (spec-first).
구현 진행에 따라 단계별로 통과한다.
"""

import time

import httpx

NOTION_PAGE_ID = "11111111-1111-1111-1111-111111111111"
TARGET_PAGE_ID = "22222222-2222-2222-2222-222222222222"
SOURCE_BLOCK_ID = "block-1"
TARGET_BLOCK_ID = "target-block-1"

WEBHOOK_PAYLOAD = {
    "id": "evt_uc3_happy_path",
    "timestamp": "2026-05-08T00:00:00Z",
    "type": "page.content_updated",
    "workspaceId": "test-workspace-id",
    "subscriptionId": "test-subscription-id",
    "entity": {"id": NOTION_PAGE_ID, "type": "page"},
    "authors": [{"id": "test-actor", "type": "person"}],
    "attemptNumber": 1,
}

NOTION_PAGE_RESPONSE = {
    "object": "page",
    "id": NOTION_PAGE_ID,
    "properties": {
        "title": {
            "title": [{"plain_text": "변경된 회의록"}],
        }
    },
}

NOTION_BLOCKS_RESPONSE = {
    "object": "list",
    "results": [
        {
            "object": "block",
            "id": "block-1",
            "type": "paragraph",
            "paragraph": {
                "rich_text": [{"plain_text": "변경된 결정사항: 출시일을 2주 미룬다"}]
            },
        }
    ],
    "next_cursor": None,
    "has_more": False,
}

def test_uc3_webhook_receipt(client):
    """webhook 수신 자체가 동작하는지. 200 OK 응답을 받는다."""
    response = client.post("/webhooks/notion", json=WEBHOOK_PAYLOAD)
    assert response.status_code == 200


def test_uc3_webhook_triggers_notion_fetch(client, wiremock):
    """webhook 수신 후 backend가 Notion API로 페이지 본문을 조회한다."""
    wiremock.stub(
        request={"method": "GET", "urlPattern": f"/v1/pages/{NOTION_PAGE_ID}"},
        response={"status": 200, "jsonBody": NOTION_PAGE_RESPONSE},
    )
    wiremock.stub(
        request={"method": "GET", "urlPattern": f"/v1/blocks/{NOTION_PAGE_ID}/children.*"},
        response={"status": 200, "jsonBody": NOTION_BLOCKS_RESPONSE},
    )

    client.post("/webhooks/notion", json=WEBHOOK_PAYLOAD)

    _wait_until(lambda: any(
        NOTION_PAGE_ID in (call.get("request", {}).get("url") or "")
        for call in wiremock.calls()
    ), reason="backend가 Notion API에 페이지 본문을 조회하지 않음")


def test_uc3_full_flow_detects_conflict(
    client, wiremock, seeder, auth_headers, stub_ai, wait_until
):
    """UC3 happy path: webhook 수신 → 본문 fetch → AI 검증 → 충돌 감지 → 인박스 노출.

    prerequisite(webhook 대상 source 문서·target 문서·의존 엣지·Notion 연결)은 fixture로 시드.
    운영에선 초기 동기화·OAuth가 이를 채운다. 검증 axis는 webhook 이후의 변경 감지 흐름.
    """
    workspace_id = seeder.workspace(
        notion_workspace_id=WEBHOOK_PAYLOAD["workspaceId"], name="UC3 Workspace"
    )
    project_id = seeder.project(workspace_id, name="UC3 Project")
    seeder.connection(notion_workspace_id=WEBHOOK_PAYLOAD["workspaceId"])
    source_doc_id = seeder.document(
        project_id, notion_page_id=NOTION_PAGE_ID, title="UC3 회의록",
        block_ids=[SOURCE_BLOCK_ID],
    )
    target_doc_id = seeder.document(
        project_id, notion_page_id=TARGET_PAGE_ID, title="UC3 요구사항",
        block_ids=[TARGET_BLOCK_ID],
    )
    seeder.edge(
        project_id=project_id,
        source_document_id=source_doc_id,
        target_document_id=target_doc_id,
    )

    wiremock.stub(
        request={"method": "GET", "urlPattern": f"/v1/pages/{NOTION_PAGE_ID}"},
        response={"status": 200, "jsonBody": NOTION_PAGE_RESPONSE},
    )
    wiremock.stub(
        request={"method": "GET", "urlPattern": f"/v1/blocks/{NOTION_PAGE_ID}/children.*"},
        response={"status": 200, "jsonBody": NOTION_BLOCKS_RESPONSE},
    )
    stub_ai([
        {
            "source_block_ids": [SOURCE_BLOCK_ID],
            "target_block_id": TARGET_BLOCK_ID,
            "rationale": "회의록의 변경된 출시일이 요구사항 문서와 어긋난다.",
            "new_text": "출시일을 2주 늦춘 일정으로 갱신",
            "title": "출시일 불일치",
        }
    ])

    client.post("/webhooks/notion", json=WEBHOOK_PAYLOAD)

    # 인박스에 충돌이 노출되는지 polling으로 확인
    wait_until(
        lambda: len(client.get("/me/conflicts", headers=auth_headers).json().get("content", [])) > 0,
        reason="인박스에 충돌이 누적되지 않음",
    )


def _wait_until(predicate, *, timeout: float = 30, interval: float = 1, reason: str = ""):
    deadline = time.time() + timeout
    while time.time() < deadline:
        try:
            if predicate():
                return
        except (httpx.HTTPError, AssertionError, KeyError, ValueError):
            pass
        time.sleep(interval)
    raise AssertionError(f"timeout after {timeout}s: {reason}")