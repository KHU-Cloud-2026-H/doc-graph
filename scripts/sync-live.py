"""실제 Notion 연동 후 초기 동기화를 backend 직접 호출로 검증.

전제: 브라우저로 실제 Notion OAuth 로그인을 마친 상태. 그 세션 쿠키(DG_SESSION)를
환경변수로 넘긴다 — sync는 그 유저의 notion_connection(실토큰)으로 Notion을 조회하므로
seed-fixture의 `/test/login` 우회(토큰 없는 acceptance 유저)로는 동작하지 않는다.

  DG_SESSION 획득: 로그인 후 브라우저 devtools → Application → Cookies → DG_SESSION 값 복사.
  주입: `.env.local`에 `DG_SESSION=...` 두면 just가 주입. 또는 인라인
        `DG_SESSION=... just sync-live ...` — 스크립트는 env에서 읽으므로 둘 다 동일.

흐름(run 모드): 프로젝트 생성 → 카테고리 매핑 → 동기화 트리거(비동기) → 그래프 노드 폴링.
discovery(--list): 워크스페이스·루트 페이지·자식 페이지를 라이브 조회해 id·매핑 결정을 돕는다.

  # 1) 후보 둘러보기
  just sync-live --list                         # 워크스페이스 + 루트 페이지
  just sync-live --list --root <rootPageId>     # 루트의 직계 자식(카테고리 후보)
  # 2) 생성·동기화·조회 (한 프로젝트가 nodes·edges까지 한 번에 출력)
  just sync-live --name Demo --root <rootPageId> \
      --category <childPageId>=design --category <childPageId>=meeting_notes \
      --rule meeting_notes=design          # 링크 방향(출발→도착) 타입쌍. 링크+룰 둘 다 있어야 엣지 생성.
  # 3) 재실행 정리 — 같은 워크스페이스+루트 중복은 백엔드가 막으므로 먼저 삭제
  just sync-live --delete <projectId>

프론트 그래프 화면이 백엔드에 붙기 전까지, 이 CLI가 live 동기화 결과(nodes/edges) 뷰어다.
NAME에 공백 금지 — just `*args`가 공백으로 토큰을 쪼갠다.
"""

import argparse
import os
import sys
import time

import httpx

DOCUMENT_TYPES = {"meeting_notes", "planning", "requirements", "design", "research"}


def _client(backend: str) -> httpx.Client:
    token = os.environ["DG_SESSION"]  # 단일 출처 — 없으면 KeyError로 즉시 실패
    return httpx.Client(
        base_url=backend,
        timeout=30.0,
        headers={"Cookie": f"DG_SESSION={token}"},
    )


def _resolve_workspace(client: httpx.Client, workspace: int | None) -> int:
    workspaces = client.get("/workspaces").raise_for_status().json()
    if not workspaces:
        sys.exit("✗ 접근 가능한 워크스페이스가 없다. OAuth 로그인이 됐는지 확인.")
    if workspace is not None:
        return workspace
    if len(workspaces) == 1:
        return workspaces[0]["id"]
    listing = "\n".join(f"    {w['id']}: {w['name']}" for w in workspaces)
    sys.exit(f"✗ 워크스페이스가 여러 개다. --workspace <id>로 지정:\n{listing}")


def _print_pages(label: str, pages: list[dict]) -> None:
    print(f"  {label}:")
    if not pages:
        print("    (없음)")
        return
    for page in pages:
        print(f"    {page['notionPageId']}  {page['title']}")


def list_mode(client: httpx.Client, workspace: int | None, root: str | None) -> None:
    ws_id = _resolve_workspace(client, workspace)
    print(f"→ workspace {ws_id}")
    if root is None:
        roots = client.get(f"/workspaces/{ws_id}/notion/root-pages").raise_for_status().json()
        _print_pages("root pages", roots)
        print("\n  카테고리 후보를 보려면: --list --root <rootPageId>")
        return
    children = (
        client.get(f"/workspaces/{ws_id}/notion/pages/{root}/children")
        .raise_for_status()
        .json()
    )
    _print_pages(f"children of {root}", children)
    print("\n  매핑 예: --category <childPageId>=planning")


def _parse_categories(raw: list[str]) -> list[tuple[str, str]]:
    parsed: list[tuple[str, str]] = []
    for item in raw:
        if "=" not in item:
            sys.exit(f"✗ --category 형식 오류 (pageId=type 이어야 함): {item}")
        page_id, doc_type = item.split("=", 1)
        if doc_type not in DOCUMENT_TYPES:
            sys.exit(f"✗ 알 수 없는 documentType '{doc_type}'. 허용: {sorted(DOCUMENT_TYPES)}")
        parsed.append((page_id, doc_type))
    return parsed


def _parse_rules(raw: list[str]) -> list[tuple[str, str, str]]:
    """`srcType=tgtType[:기준]` → (source, target, criterion). 엣지는 link + 이 타입쌍 룰이 둘 다 있을 때만 생긴다."""
    parsed: list[tuple[str, str, str]] = []
    for item in raw:
        pair, _, criterion = item.partition(":")
        if "=" not in pair:
            sys.exit(f"✗ --rule 형식 오류 (srcType=tgtType[:기준]): {item}")
        src, tgt = pair.split("=", 1)
        for t in (src, tgt):
            if t not in DOCUMENT_TYPES:
                sys.exit(f"✗ 알 수 없는 documentType '{t}'. 허용: {sorted(DOCUMENT_TYPES)}")
        if src == tgt:
            sys.exit(f"✗ 룰 source/target 타입은 달라야 한다: {item}")
        parsed.append((src, tgt, criterion or "live 검증 룰"))
    return parsed


def run_mode(args: argparse.Namespace) -> None:
    categories = _parse_categories(args.category)
    rules = _parse_rules(args.rule)
    if not categories:
        print("⚠ --category 없음 — 문서 타입 미분류로 동기화된다. 노드는 생기되 엣지는 안 생긴다.")
    elif not rules:
        print("⚠ --rule 없음 — Notion 링크가 있어도 타입쌍 룰이 없으면 엣지는 안 생긴다.")

    with _client(args.backend) as client:
        ws_id = _resolve_workspace(client, args.workspace)

        print(f"→ 프로젝트 생성 (workspace {ws_id}, root {args.root})")
        project_id = (
            client.post(
                f"/workspaces/{ws_id}/projects",
                json={"name": args.name, "notionRootPageId": args.root},
            )
            .raise_for_status()
            .json()["id"]
        )
        print(f"  project_id: {project_id}")

        for page_id, doc_type in categories:
            print(f"→ 카테고리 {page_id} = {doc_type}")
            client.post(
                f"/projects/{project_id}/categories",
                json={"notionPageId": page_id, "documentType": doc_type},
            ).raise_for_status()

        for src, tgt, criterion in rules:
            print(f"→ 룰 {src} → {tgt} ({criterion})")
            client.post(
                f"/projects/{project_id}/rules",
                json={"sourceType": src, "targetType": tgt, "validationCriterion": criterion},
            ).raise_for_status()

        print("→ 동기화 트리거 (비동기 — worker가 Notion 트리 조회)")
        client.post(f"/projects/{project_id}/sync").raise_for_status()

        print(f"→ 그래프 폴링 (timeout {args.timeout}s)")
        graph = _poll_graph(client, project_id, args.timeout)

    _print_graph(graph, args.backend, project_id)


def _print_graph(graph: dict, backend: str, project_id: int) -> None:
    nodes, edges, proposals = graph["nodes"], graph["edges"], graph["proposals"]
    title = {n["id"]: n["title"] for n in nodes}

    def label(doc_id: int) -> str:
        return title.get(doc_id, f"#{doc_id}")

    print()
    print(f"✓ 동기화 완료 — nodes {len(nodes)} · edges {len(edges)} · proposals {len(proposals)}")
    print("  nodes:")
    for node in nodes:
        print(f"    [{node['id']}] {node['title']}  ({node['type'] or '미분류'})")
    if edges:
        print("  edges:")
        for e in edges:
            print(
                f"    {label(e['sourceDocumentId'])} → {label(e['targetDocumentId'])}"
                f"  [{e['conflictStatus']}] ({e['validationCriterion']})"
            )
    if proposals:
        print("  proposals:")
        for p in proposals:
            print(f"    {label(p['sourceDocumentId'])} → {label(p['targetDocumentId'])}  sim={p['similarityScore']}")
    print(f"\n  재조회: GET {backend}/projects/{project_id}/graph")


def page_mode(backend: str, workspace: int | None, page_id: str) -> None:
    """Notion 페이지 본문 라이브 조회 (재현 환경 문서화·확인용)."""
    with _client(backend) as client:
        ws_id = _resolve_workspace(client, workspace)
        content = (
            client.get(f"/workspaces/{ws_id}/notion/pages/{page_id}/content")
            .raise_for_status()
            .json()
        )
    print(f"[{content.get('id', page_id)}] {content.get('title', '')}")
    print(content.get("flatText") or "(본문 없음)")


def inspect_mode(backend: str, project_id: int) -> None:
    """LLM 충돌 검증 결과 조회 — 검증 작업 상태(SUCCESS/PENDING/FAILED) + 검출된 충돌."""
    with _client(backend) as client:
        tasks = client.get(f"/projects/{project_id}/validation-tasks").raise_for_status().json()["content"]
        conflicts = client.get(f"/projects/{project_id}/conflicts").raise_for_status().json()["content"]
    print(f"validation-tasks ({len(tasks)}):  ← LLM 실행 상태")
    for t in tasks:
        print(f"    task {t['id']}  edge {t['edgeId']}  {t['status']}  {t['createdAt']}")
    print(f"conflicts ({len(conflicts)}):  ← LLM이 검출한 충돌")
    if not conflicts:
        print("    (없음 — 검증은 됐으나 충돌 없음. 충돌을 보려면 링크된 두 문서가 criterion상 실제로 어긋나야 함)")
    for c in conflicts:
        print(f"    conflict {c['id']}  edge {c['edgeId']}  doc {c['sourceDocumentId']}→{c['targetDocumentId']}  findings={len(c['findings'])}")
        for f in c["findings"]:
            print(f"        - {f}")


def delete_mode(backend: str, project_id: int) -> None:
    with _client(backend) as client:
        client.delete(f"/projects/{project_id}").raise_for_status()
    print(f"✓ project {project_id} 삭제")


def _poll_graph(client: httpx.Client, project_id: int, timeout: int) -> dict:
    deadline = time.time() + timeout
    while time.time() < deadline:
        graph = client.get(f"/projects/{project_id}/graph").raise_for_status().json()
        if graph["nodes"]:
            return graph
        time.sleep(3)
    sys.exit(f"✗ {timeout}s 내 노드 없음. backend 로그에서 worker/Notion 호출 실패 확인.")


def main() -> None:
    parser = argparse.ArgumentParser(description="실제 Notion 초기 동기화 검증")
    parser.add_argument("--backend", default="http://localhost:8080/api")
    parser.add_argument("--workspace", type=int, help="미지정 시 단일 워크스페이스 자동 선택")
    parser.add_argument("--list", action="store_true", help="워크스페이스·페이지 둘러보기")
    parser.add_argument("--root", help="Notion 루트 페이지 ID (run) 또는 자식 조회 대상 (--list)")
    parser.add_argument("--name", help="생성할 프로젝트 이름 (run 모드 필수)")
    parser.add_argument(
        "--category",
        action="append",
        default=[],
        metavar="pageId=type",
        help="카테고리 매핑 (반복). type ∈ " + ", ".join(sorted(DOCUMENT_TYPES)),
    )
    parser.add_argument(
        "--rule",
        action="append",
        default=[],
        metavar="srcType=tgtType[:기준]",
        help="엣지 생성 룰 (반복). 예: design=meeting_notes. 링크 방향(출발 타입→도착 타입)과 일치해야 엣지 생성.",
    )
    parser.add_argument("--timeout", type=int, default=120, help="그래프 폴링 타임아웃(초)")
    parser.add_argument("--delete", type=int, metavar="projectId", help="프로젝트 삭제(재실행 정리용)")
    parser.add_argument("--inspect", type=int, metavar="projectId", help="LLM 충돌 검증 결과 조회")
    parser.add_argument("--page", metavar="pageId", help="Notion 페이지 본문 라이브 조회")
    args = parser.parse_args()

    if args.delete is not None:
        delete_mode(args.backend, args.delete)
        return

    if args.inspect is not None:
        inspect_mode(args.backend, args.inspect)
        return

    if args.page is not None:
        page_mode(args.backend, args.workspace, args.page)
        return

    if args.list:
        with _client(args.backend) as client:
            list_mode(client, args.workspace, args.root)
        return

    if not args.name or not args.root:
        parser.error("run 모드는 --name 과 --root 가 필요하다 (둘러보기는 --list).")
    run_mode(args)


if __name__ == "__main__":
    try:
        main()
    except KeyError as e:
        sys.exit(f"✗ 환경변수 {e} 없음. 브라우저 로그인 후 DG_SESSION 쿠키값을 넘겨라.")
    except httpx.HTTPStatusError as e:
        sys.exit(f"✗ HTTP {e.response.status_code} ({e.request.url}): {e.response.text}")
    except httpx.RequestError as e:
        sys.exit(f"✗ 요청 실패: {e} (backend 띄워졌나?)")