"""OAuth/frontend 부재 시 backend 직접 호출을 위한 임시 fixture seed.

fixture endpoint(`POST /test/*`)로 workspace/project/document/edge/proposal/conflict를
박는다. OAuth backend·frontend가 합류하면 본 스크립트는 불필요.

backend는 acceptance profile로 띄워져 있어야 한다 (`just bootRun acceptance` 또는
SPRING_PROFILES_ACTIVE=acceptance). re-seed 시 `POST /test/reset`으로 비-Flyway
테이블 비운 후 재박음 (idempotent).

인증: 운영과 동일한 세션 쿠키. `POST /test/reset` 후 `GET /test/login`으로 정규 acceptance 유저
세션을 발급받아 httpx 쿠키 jar에 보관 — 이후 호출에 자동 첨부. 브라우저도 `/api/test/login` 진입 시
동일 유저로 로그인되어 본 스크립트가 박은 데이터를 본다.
"""

import argparse
import sys

import httpx


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--backend", default="http://localhost:8080/api")
    args = parser.parse_args()

    client = httpx.Client(base_url=args.backend, timeout=10.0)

    print("→ reset")
    client.post("/test/reset").raise_for_status()

    print("→ login (세션 쿠키)")
    login = client.get("/test/login")
    assert login.status_code in (200, 302), f"login 실패: {login.status_code}"
    # 302 Set-Cookie의 DG_SESSION이 client.cookies jar에 저장되어 이후 호출에 자동 첨부됨.

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
    for slug, title, dtype, blocks in _documents():
        doc_ids[slug] = _post_doc(client, project_id, slug, title, dtype, blocks)

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
            "title": "기획서·요구사항 명세 간 출시일 불일치",
            "rationale": "기획서의 출시일과 요구사항 문서가 어긋남",
            "newText": "출시일을 2주 미룬다 (요구사항 반영)",
        }],
    })

    print()
    print(f"✓ seeded. backend: {args.backend}")
    print(f"  workspace_id: {ws_id}")
    print(f"  project_id: {project_id}")
    print("  브라우저 로그인: GET /api/test/login (동일 정규 유저 세션 쿠키)")
    print("  인박스 조회: GET /me/conflicts")
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


def _post_doc(
    client: httpx.Client,
    project_id: int,
    slug: str,
    title: str,
    dtype: str,
    blocks: list[dict],
) -> int:
    response = client.post("/test/documents", json={
        "projectId": project_id,
        "notionPageId": f"sample-{slug}",
        "title": title,
        "type": dtype,
        "blocks": blocks,
    })
    response.raise_for_status()
    return response.json()["id"]


def _b(
    block_id: str,
    block_type: str,
    order: int,
    text: str | None = None,
    parent: str | None = None,
) -> dict:
    """블록 fixture 한 줄.

    parent 지정 시 parentType=block_id로 중첩 — 프론트 BlockRenderer가 parentBlockId로 트리를 복원한다.
    text 미지정(table·image·divider 등)은 실제 Notion sync가 text=null로 내려주는 동작과 일치.
    """
    item: dict = {"notionBlockId": block_id, "type": block_type, "sortOrder": order}
    if text is not None:
        item["text"] = text
    if parent is not None:
        item["parentType"] = "block_id"
        item["parentId"] = parent
    return item


def _documents() -> list[tuple[str, str, str, list[dict]]]:
    """문서 타입별 현실적 블록 구성. 프론트 BlockRenderer의 모든 type 분기를 한 번씩 커버한다.

    각 문서 첫 콘텐츠 블록 id는 `{slug}-block-1` 유지 — conflict fixture가 이 id를 참조한다.
    """
    return [
        ("planning", "기획서", "planning", [
            _b("planning-h1", "heading_1", 0, "WorkSync 기획서"),
            _b("planning-block-1", "paragraph", 1, "핵심 범위는 출퇴근 기록·근태 리포트 자동화이며, 목표 출시일은 2026-07-01이다."),
            _b("planning-h2-bg", "heading_2", 2, "1. 배경"),
            _b("planning-bg", "paragraph", 3, "수기 출퇴근 기록의 오류와 관리 비용을 줄이는 것을 목적으로 한다."),
            _b("planning-quote", "quote", 4, "출근 09:00, 퇴근 18:00 — 표준 근무 정책을 모든 산정의 기준으로 삼는다."),
            _b("planning-h3-goal", "heading_3", 5, "1.1 목표"),
            _b("planning-goal-1", "bulleted_list_item", 6, "출퇴근 기록을 자동으로 수집·저장한다."),
            _b("planning-goal-2", "bulleted_list_item", 7, "지각·조퇴를 정책에 따라 자동 산정한다."),
            _b("planning-goal-2-1", "bulleted_list_item", 0, "09:11:00부터 지각(LATE)으로 처리한다.", parent="planning-goal-2"),
            _b("planning-callout", "callout", 8, "우선순위는 매 분기 회고에서 재검토한다."),
        ]),
        ("requirements", "요구사항 명세", "requirements", [
            _b("requirements-h1", "heading_1", 0, "요구사항 명세"),
            _b("requirements-block-1", "paragraph", 1, "출시일은 2026-06-15로 확정한다."),
            _b("requirements-h2-func", "heading_2", 2, "1. 기능 요구사항"),
            _b("requirements-func-1", "numbered_list_item", 3, "출퇴근 체크인/체크아웃"),
            _b("requirements-func-2", "numbered_list_item", 4, "실시간 근태 현황 대시보드"),
            _b("requirements-func-3", "numbered_list_item", 5, "휴가 신청 및 결재 워크플로"),
            _b("requirements-h4-note", "heading_4", 6, "1.1 비고"),
            _b("requirements-note", "paragraph", 7, "기능 우선순위는 분기마다 갱신한다."),
            _b("requirements-h2-data", "heading_2", 8, "2. 데이터 모델"),
            _b("requirements-table", "table", 9),
            _b("requirements-divider", "divider", 10),
            _b("requirements-todo-1", "to_do", 11, "비기능 요구사항 리뷰"),
            _b("requirements-todo-2", "to_do", 12, "이해관계자 승인 받기"),
        ]),
        ("design", "설계 문서", "design", [
            _b("design-h1", "heading_1", 0, "설계 문서"),
            _b("design-block-1", "paragraph", 1, "시스템 아키텍처와 API 계약을 기술한다."),
            _b("design-h2-api", "heading_2", 2, "1. API"),
            _b("design-code", "code", 3, "GET /documents/{id}\n→ { title, type, blocks[] }"),
            _b("design-h2-pages", "heading_2", 4, "2. 관련 페이지"),
            _b("design-child-page", "child_page", 5, "데이터베이스 스키마 설계"),
            _b("design-child-db", "child_database", 6, "이슈 트래커"),
            _b("design-image", "image", 7),
            _b("design-h2-history", "heading_2", 8, "3. 변경 이력"),
            _b("design-toggle", "toggle", 9, "변경 이력 펼치기"),
            _b("design-history-1", "paragraph", 0, "2026-05-21: 초안 작성", parent="design-toggle"),
            _b("design-history-2", "paragraph", 1, "2026-05-28: 핵심 기능 우선순위 갱신", parent="design-toggle"),
        ]),
        ("meeting", "킥오프 회의록", "meeting_notes", [
            _b("meeting-h1", "heading_1", 0, "킥오프 회의록"),
            _b("meeting-block-1", "paragraph", 1, "2026-05-20 프로젝트 킥오프 회의."),
            _b("meeting-h3-attendees", "heading_3", 2, "참석자"),
            _b("meeting-attendees", "bulleted_list_item", 3, "PM · 백엔드 · 프론트엔드 · 디자인"),
            _b("meeting-h3-decisions", "heading_3", 4, "결정 사항"),
            _b("meeting-decision-1", "to_do", 5, "출시일 2주 연기 합의"),
            _b("meeting-decision-2", "to_do", 6, "MVP 범위를 출퇴근·대시보드로 한정"),
            _b("meeting-quote", "quote", 7, "다음 정기 회의는 2026-05-27."),
        ]),
    ]


if __name__ == "__main__":
    try:
        main()
    except httpx.HTTPStatusError as e:
        print(f"✗ HTTP {e.response.status_code}: {e.response.text}", file=sys.stderr)
        sys.exit(1)
    except httpx.RequestError as e:
        print(f"✗ request failed: {e} (backend 띄워졌나? acceptance profile?)", file=sys.stderr)
        sys.exit(1)
