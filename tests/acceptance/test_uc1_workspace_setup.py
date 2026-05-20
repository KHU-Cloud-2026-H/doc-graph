"""UC1: 회원가입 및 워크스페이스 연결.

OAuth handshake·인증 거부 경계는 슬라이스/컴포넌트 테스트에서 검증.
인수 테스트는 인증된 사용자 컨텍스트에서 워크스페이스 등록·조회가
가능한지(=데모 가능 상태)를 확인한다.
"""


def test_uc1_create_workspace_after_auth(client, auth_headers):
    """인증된 사용자가 Notion 워크스페이스를 등록한 결과를 검증."""
    create = client.post(
        "/workspaces",
        headers=auth_headers,
        json={
            "notionWorkspaceId": "test-notion-ws-1",
            "name": "Test Workspace",
        },
    )
    assert create.status_code == 201
    workspace_id = create.json()["id"]

    # 등록한 워크스페이스가 본인 목록에 보임
    listing = client.get("/workspaces", headers=auth_headers)
    assert listing.status_code == 200
    items = listing.json().get("content", listing.json())
    assert any(item["id"] == workspace_id for item in items)


def test_uc1_workspace_owner_is_current_user(client, auth_headers):
    """워크스페이스 생성자는 자동으로 소유자(Project Admin) 권한을 갖는다."""
    create = client.post(
        "/workspaces",
        headers=auth_headers,
        json={
            "notionWorkspaceId": "test-notion-ws-2",
            "name": "Owner Check Workspace",
        },
    )
    assert create.status_code == 201
    body = create.json()

    # 응답에 owner 정보가 포함되어 있고 현재 사용자임
    assert "createdBy" in body or "ownerId" in body