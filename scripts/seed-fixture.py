"""OAuth/frontend 부재 시 backend 직접 호출을 위한 임시 fixture seed.

fixture endpoint(`POST /test/*`)로 workspace/project/document/edge/proposal/conflict를
박는다. OAuth backend·frontend가 합류하면 본 스크립트는 불필요.

backend는 acceptance profile로 띄워져 있어야 한다 (`just bootRun acceptance` 또는
SPRING_PROFILES_ACTIVE=acceptance). re-seed 시 `POST /test/reset`으로 비-Flyway
테이블 비운 후 재박음 (idempotent).

인증: 모든 호출에 `X-Test-User-Id: 1` 헤더 사용 (`AcceptanceGetCurrentUserIdQueryHandler`가
헤더 값을 그대로 user id로 변환).
"""

import argparse
import sys

import httpx

HEADERS = {"X-Test-User-Id": "1"}


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--backend", default="http://localhost:8080/api")
    args = parser.parse_args()

    client = httpx.Client(base_url=args.backend, headers=HEADERS, timeout=10.0)

    print("→ reset")
    client.post("/test/reset").raise_for_status()

    print("→ workspace")
    ws_id = _post(client, "/test/workspaces", {
        "notionWorkspaceId": "sample-notion-ws",
        "name": "Sample Workspace",
    })

    print("→ project")
    project_id = _post(client, "/test/projects", {
        "workspaceId": ws_id,
        "name": "Sample Project",
    })

    print("→ documents")
    doc_ids: dict[str, int] = {}
    for slug, title, dtype in [
        ("planning", "기획서", "planning"),
        ("requirements", "요구사항 명세", "requirements"),
        ("design", "설계 문서", "design"),
        ("meeting", "킥오프 회의록", "meeting_notes"),
    ]:
        doc_ids[slug] = _post_doc(client, project_id, slug, title, dtype)

    print("→ edge (planning → requirements)")
    edge_id = _post(client, "/test/edges", {
        "projectId": project_id,
        "sourceDocumentId": doc_ids["planning"],
        "targetDocumentId": doc_ids["requirements"],
        "validationCriterion": "기획 범위와 요구사항 일치 여부",
        "source": "CUSTOM",
    })

    print("→ proposal (meeting → design)")
    _post(client, "/test/proposals", {
        "projectId": project_id,
        "sourceDocumentId": doc_ids["meeting"],
        "targetDocumentId": doc_ids["design"],
        "validationCriterion": "회의록 결정과 설계 정합",
        "similarityScore": 0.78,
    })

    print("→ conflict (active, planning ↔ requirements)")
    _post(client, "/test/conflicts", {
        "edgeId": edge_id,
        "findings": [{
            "sourceBlockIds": ["planning-block-1"],
            "targetBlockId": "requirements-block-1",
            "rationale": "기획서의 출시일과 요구사항 문서가 어긋남",
            "newText": "출시일을 2주 미룬다 (요구사항 반영)",
        }],
    })

    print()
    print(f"✓ seeded. backend: {args.backend}")
    print(f"  workspace_id: {ws_id}")
    print(f"  project_id: {project_id}")
    print("  X-Test-User-Id 헤더: 1")
    print("  인박스 조회: GET /me/conflicts (헤더 포함)")
    print("  그래프 조회: GET /projects/{}/graph".format(project_id))


def _post(client: httpx.Client, path: str, body: dict) -> int:
    response = client.post(path, json=body)
    response.raise_for_status()
    payload = response.json()
    if "id" in payload:
        return payload["id"]
    if "memberId" in payload:
        return payload["memberId"]
    raise ValueError(f"{path}: no id in response {payload}")


def _post_doc(client: httpx.Client, project_id: int, slug: str, title: str, dtype: str) -> int:
    response = client.post("/test/documents", json={
        "projectId": project_id,
        "notionPageId": f"sample-{slug}",
        "title": title,
        "type": dtype,
        "blocks": [
            {"notionBlockId": f"{slug}-block-1", "type": "paragraph", "sortOrder": 0},
        ],
    })
    response.raise_for_status()
    return response.json()["id"]


if __name__ == "__main__":
    try:
        main()
    except httpx.HTTPStatusError as e:
        print(f"✗ HTTP {e.response.status_code}: {e.response.text}", file=sys.stderr)
        sys.exit(1)
    except httpx.RequestError as e:
        print(f"✗ request failed: {e} (backend 띄워졌나? acceptance profile?)", file=sys.stderr)
        sys.exit(1)
