"""인수 테스트 conftest — 조건부 wiremock·DB reset·auth headers.

local-mock 모드: WireMockHelper가 실제 stub·reset 수행.
local-live·remote-live 모드: NoopWireMock — stub·reset이 no-op이고 실제
외부 API가 응답한다. acceptance 시나리오 코드는 모드 무관하게 동일.
"""

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
    return {"X-Test-User-Id": "test-user-1"}
