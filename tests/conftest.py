"""공통 conftest — --env flag, env_config, client.

acceptance·system 두 레벨이 공유하는 base URL·HTTP client만 정의한다.
wiremock·DB reset 같은 acceptance 전용 fixture는 tests/acceptance/conftest.py.
"""

import os

import httpx
import pytest


def pytest_addoption(parser):
    parser.addoption(
        "--env",
        default="local-mock",
        choices=["local-mock", "local-live", "remote-live"],
        help="acceptance·system 테스트 실행 모드",
    )


@pytest.fixture(scope="session")
def env_config(request) -> dict:
    return _load_config(request.config.getoption("--env"))


def _load_config(env: str) -> dict:
    if env == "local-mock":
        return {
            "backend": os.environ["ACCEPTANCE_LOCAL_BACKEND_URL"],
            "wiremock_enabled": True,
            "wiremock": os.environ["ACCEPTANCE_LOCAL_WIREMOCK_URL"],
        }
    if env == "local-live":
        return {
            "backend": os.environ["ACCEPTANCE_LOCAL_BACKEND_URL"],
            "wiremock_enabled": False,
        }
    if env == "remote-live":
        url = os.environ["ACCEPTANCE_REMOTE_BACKEND_URL"]
        if not url:
            raise pytest.UsageError("ACCEPTANCE_REMOTE_BACKEND_URL is not set")
        return {"backend": url, "wiremock_enabled": False}
    raise pytest.UsageError(f"unknown env: {env}")


@pytest.fixture(scope="session")
def client(env_config):
    with httpx.Client(base_url=env_config["backend"]) as c:
        yield c
