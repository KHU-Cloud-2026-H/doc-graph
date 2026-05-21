"""UC1: 회원가입 및 워크스페이스 연결.

OAuth handshake·인증 거부 경계는 슬라이스/컴포넌트 테스트에서 검증.
인수 테스트는 등록된 워크스페이스가 본인 listing에 보이는지 = 데모 진입 가능 상태를 확인한다.

운영 등록 흐름은 OAuth listener → workspace_member 동시 박힘. acceptance는
`POST /test/workspaces` fixture로 우회 (스코프 외 OAuth handshake 검증 X).
"""


def test_uc1_workspace_appears_in_listing_after_setup(client, auth_headers, seeder):
    """등록된 워크스페이스가 본인 접근 listing에 보임 = 데모 진입 가능."""
    workspace_id = seeder.workspace(
        notion_workspace_id="test-notion-ws-1",
        name="Test Workspace",
    )

    listing = client.get("/workspaces", headers=auth_headers)
    assert listing.status_code == 200
    items = listing.json()
    assert any(item["id"] == workspace_id for item in items)
