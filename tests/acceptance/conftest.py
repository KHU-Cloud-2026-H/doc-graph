"""인수 테스트 conftest — 조건부 wiremock·DB reset·auth headers·fixture seed helper.

local-mock 모드: WireMockHelper가 실제 stub·reset 수행.
local-live·remote-live 모드: NoopWireMock — stub·reset이 no-op이고 실제
외부 API가 응답한다. acceptance 시나리오 코드는 모드 무관하게 동일.

Seeder는 UC4~UC8 spec이 검증 대상 외 prerequisite을 acceptance fixture
endpoint(/test/...)로 우회 생성할 때 사용한다.
"""

import time

import httpx
import pytest


class WireMockHelper:
    def __init__(self, base_url: str):
        self.base_url = base_url

    def stub(self, request: dict, response: dict) -> httpx.Response:
        return httpx.post(
            f"{self.base_url}/__admin/mappings",
            json={"request": request, "response": response},
        )

    def reset(self) -> None:
        httpx.post(f"{self.base_url}/__admin/reset")

    def calls(self) -> list:
        return httpx.get(f"{self.base_url}/__admin/requests").json().get("requests", [])


class NoopWireMock:
    def stub(self, request: dict, response: dict) -> None:
        return None

    def reset(self) -> None:
        return None

    def calls(self) -> list:
        return []


@pytest.fixture(scope="session")
def wiremock(env_config):
    if env_config.get("wiremock_enabled"):
        return WireMockHelper(env_config["wiremock"])
    return NoopWireMock()


@pytest.fixture(autouse=True)
def _isolate(client, wiremock):
    wiremock.reset()
    client.post("/test/reset")
    yield


@pytest.fixture
def auth_headers():
    return {"X-Test-User-Id": "1"}


class Seeder:
    """acceptance fixture endpoint(/test/...) 우회 호출 helper."""

    def __init__(self, client, headers: dict):
        self.client = client
        self.headers = headers

    def workspace(self, *, notion_workspace_id: str, name: str) -> int:
        response = self.client.post(
            "/test/workspaces",
            headers=self.headers,
            json={"notionWorkspaceId": notion_workspace_id, "name": name},
        )
        assert response.status_code in (200, 201)
        return response.json()["id"]

    def project(
        self,
        workspace_id: int,
        *,
        name: str,
        notion_root_page_id: str | None = None,
    ) -> int:
        body: dict = {"workspaceId": workspace_id, "name": name}
        if notion_root_page_id is not None:
            body["notionRootPageId"] = notion_root_page_id
        response = self.client.post("/test/projects", headers=self.headers, json=body)
        assert response.status_code in (200, 201)
        return response.json()["id"]

    def document(
        self,
        project_id: int,
        *,
        notion_page_id: str,
        title: str,
        document_type: str | None = None,
        block_ids: list[str] | None = None,
    ) -> int:
        body: dict = {
            "projectId": project_id,
            "notionPageId": notion_page_id,
            "title": title,
        }
        if document_type is not None:
            body["documentType"] = document_type
        if block_ids is not None:
            body["blockIds"] = block_ids
        response = self.client.post("/test/documents", headers=self.headers, json=body)
        assert response.status_code in (200, 201)
        return response.json()["id"]

    def edge(self, *, source_document_id: int, target_document_id: int) -> int:
        response = self.client.post(
            "/test/edges",
            headers=self.headers,
            json={
                "sourceDocumentId": source_document_id,
                "targetDocumentId": target_document_id,
            },
        )
        assert response.status_code in (200, 201)
        return response.json()["id"]

    def proposal(
        self, *, project_id: int, source_document_id: int, target_document_id: int
    ) -> int:
        response = self.client.post(
            "/test/proposals",
            headers=self.headers,
            json={
                "projectId": project_id,
                "sourceDocumentId": source_document_id,
                "targetDocumentId": target_document_id,
            },
        )
        assert response.status_code in (200, 201)
        return response.json()["id"]

    def conflict(self, *, edge_id: int, findings: list[dict]) -> dict:
        """응답 형태: {id, findings: [{id, ...}, ...]}."""
        response = self.client.post(
            "/test/conflicts",
            headers=self.headers,
            json={"edgeId": edge_id, "findings": findings},
        )
        assert response.status_code in (200, 201)
        return response.json()

    def workspace_member(self, *, workspace_id: int) -> int:
        response = self.client.post(
            "/test/workspace-members",
            headers=self.headers,
            json={"workspaceId": workspace_id},
        )
        assert response.status_code in (200, 201)
        return response.json()["memberId"]

    def project_member(
        self, *, project_id: int, workspace_member_id: int, role: str = "MEMBER"
    ) -> None:
        response = self.client.post(
            "/test/project-members",
            headers=self.headers,
            json={
                "projectId": project_id,
                "workspaceMemberId": workspace_member_id,
                "role": role,
            },
        )
        assert response.status_code in (200, 201)


@pytest.fixture
def seeder(client, auth_headers):
    return Seeder(client, auth_headers)


@pytest.fixture
def inbox_has(client):
    def _has(conflict_id: int, *, headers: dict) -> bool:
        response = client.get("/me/conflicts", headers=headers)
        if response.status_code != 200:
            return False
        items = response.json().get("content", [])
        return any(item.get("id") == conflict_id for item in items)

    return _has


@pytest.fixture
def wait_until():
    def _wait(predicate, *, timeout: float = 30, interval: float = 1, reason: str = ""):
        deadline = time.time() + timeout
        while time.time() < deadline:
            try:
                if predicate():
                    return
            except (httpx.HTTPError, AssertionError, KeyError, ValueError):
                pass
            time.sleep(interval)
        raise AssertionError(f"timeout after {timeout}s: {reason}")

    return _wait
