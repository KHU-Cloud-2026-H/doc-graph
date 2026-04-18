import subprocess
import time
from pathlib import Path

import httpx
import pytest

PROJECT_ROOT = Path(__file__).parent.parent
BASE_URL = "http://localhost:8080"


COMPOSE_CMD = [
    "docker", "compose",
    "-f", "docker-compose.yml",
    "-f", "docker-compose.test.yml",
    "--profile", "full",
]


@pytest.fixture(scope="session", autouse=True)
def docker_stack():
    subprocess.run([*COMPOSE_CMD, "up", "-d"], cwd=PROJECT_ROOT, check=True)
    _wait_for_backend()
    yield
    subprocess.run([*COMPOSE_CMD, "down"], cwd=PROJECT_ROOT, check=True)


@pytest.fixture(scope="session")
def client():
    with httpx.Client(base_url=BASE_URL) as c:
        yield c


def _wait_for_backend(timeout: int = 120):
    deadline = time.time() + timeout
    while time.time() < deadline:
        try:
            if httpx.get(f"{BASE_URL}/actuator/health", timeout=2).status_code == 200:
                return
        except Exception:
            pass
        time.sleep(3)
    raise TimeoutError("Backend did not become healthy in time")
