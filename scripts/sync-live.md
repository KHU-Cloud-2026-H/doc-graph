# sync-live — 실제 Notion 초기 동기화·충돌 검증 CLI (임시)

프론트 위저드·그래프 화면이 백엔드에 붙기 전까지, 실제 Notion 연동(동기화 → 엣지 → 충돌 검증)을 CLI로 돌려보는 임시 도구.

대상 스크립트: `scripts/sync-live.py` · 진입점: `just sync-live`

## 사전 준비

1. **백엔드 기동** — `just compose-up live` (postgres + backend + ngrok, 실제 Notion API). ngrok은 재현 ③(webhook) 수신용.
2. **프론트 기동** — `just dev-frontend` (Vite dev server `:5173`, `/api`는 `:8080`으로 proxy).
3. **로그인 → 세션 쿠키**
   - 브라우저로 `http://localhost:5173` 진입 → 로그인 → Notion 동의 화면에서 동기화할 페이지 선택·허용 → `/workspaces`로 복귀 (프론트가 mock이라 화면은 미완이지만 세션은 발급됨).
   - devtools → Application → Cookies → `http://localhost:5173` → `DG_SESSION` 값 복사 (httpOnly라 콘솔엔 안 보임; 쿠키는 포트 무관이라 `:8080`에서도 동일 값).
4. **환경변수** (`.env.local`에 넣거나 인라인 env로 전달 — 스크립트가 env에서 읽음):
   - `AI_OPENAI_API_KEY` — AI provider 키. `AI_OPENAI_BASE_URL`·`AI_OPENAI_MODEL`은 `.env` 기본값 사용 (다른 provider 쓰려면 둘도 override).
   - `DG_SESSION` — 2번에서 복사한 세션 쿠키 값 (TTL 있음, 만료 시 재로그인).

## 명령

| 목적 | 명령 |
| --- | --- |
| 워크스페이스·루트 페이지 목록 | `just sync-live --list` |
| 루트의 직계 자식 목록 | `just sync-live --list --root {루트 id}` |
| 페이지 본문 조회 | `just sync-live --page {페이지 id}` |
| 생성 → 동기화 → 그래프 출력 | `just sync-live --name {name} --root {루트 id} --category {pid}={type} --rule {src}={tgt}` |
| 충돌 검증 결과 조회 | `just sync-live --inspect {projectId}` |
| 프로젝트 삭제(재실행 정리) | `just sync-live --delete {projectId}` |

- `--category` / `--rule`는 반복 가능. `type` ∈ `meeting_notes` `planning` `requirements` `design` `research`.
- `--rule {src}={tgt}` 방향은 Notion 링크 방향(출발 타입 → 도착 타입)과 일치해야 엣지 생성.
- `--name`에 공백 금지. 워크스페이스 1개면 자동 선택, 여러 개면 `--workspace {id}`.

## 재현 ① — 초기 세팅

**환경변수** — 이번 재현의 `.env.local` (키 마스킹):

```dotenv
AI_OPENAI_BASE_URL=https://openrouter.ai/api/v1
AI_OPENAI_MODEL=deepseek/deepseek-v4-flash
AI_OPENAI_API_KEY=sk-or-v1-…        # OpenRouter 키
DG_SESSION=…                        # 로그인 후 복사 (TTL)
```

**Notion 트리**:

```
루트
├─ 설계서     본문: ND_COUNT=1로 정한다.
└─ 회의록1    본문: [[설계서]]의 ND_COUNT를 2로 변경한다.
```

- `[[설계서]]`는 설계서 페이지 멘션 링크 → 회의록1 → 설계서 연결의 출처.
- 두 본문이 `ND_COUNT`를 1 vs 2로 어긋나게 둔 것이 충돌 검출 대상.

## 재현 ② — 실행

1. 페이지 id 확인:
   ```bash
   just sync-live --list                    # 루트 id
   just sync-live --list --root {루트 id}    # 설계서·회의록1 id
   ```
2. 프로젝트 생성 + 타입 매핑 + 룰 + 동기화:
   ```bash
   just sync-live --name Demo \
       --root {루트 id} \
       --category {설계서 id}=design \
       --category {회의록1 id}=meeting_notes \
       --rule meeting_notes=design
   ```
   - 설계서 → `design`, 회의록1 → `meeting_notes`. 룰 `meeting_notes=design`이 링크 방향과 일치 → 엣지 생성.
   - 출력: nodes 3 · edges 1(회의록1 → 설계서).
3. 충돌 검증 결과:
   ```bash
   just sync-live --inspect {projectId}
   ```
   - 기대: 검증 `SUCCESS`, 충돌 1건 — "ND_COUNT 값이 source(2)와 target(1) 간 불일치", 제안 수정 = 설계서의 ND_COUNT를 2로.

재실행 시 같은 루트로는 중복 생성이 막히므로 `just sync-live --delete {projectId}` 후 ② 반복.

## 재현 ③ — webhook 실수신

재현 ②의 `Demo`(회의록1 → 설계서)에 이어, 실제 편집이 webhook → 변경 감지 → AI 검증 → 담당자 인박스로 흐르는지 본다. `--inspect` 직접 트리거 대신 실제 이벤트 경로.

**ngrok·구독**:

- `just compose-up live`가 백엔드를 공유 도메인 `https://{NGROK_STATIC_DOMAIN}`에 노출. 수신은 인스펙터 `http://localhost:{NGROK_HOST_PORT}`에서 확인.
- 그 도메인의 `/api/webhooks/notion` subscription은 공유 integration에 이미 등록돼 있고 secret은 `.env`의 `NOTION_WEBHOOK_SECRET` — 재현 시 추가 작업 없음.

**실행**:

1. OAuth 로그인 `http://localhost:8080/api/oauth2/authorization/notion` → 재현 ②로 sync (연결·sync 전 편집은 orphan 처리).
2. Notion UI에서 사람이 회의록1 편집·저장 (API·bot 편집은 무시).
3. 인스펙터(200)·backend 로그로 수신 확인.
4. `just sync-live --inspect {projectId}` 또는 `GET /api/me/conflicts`(담당자 인박스). 기대: 충돌 1건 ACTIVE.

## 재현 ④ — 제안 수락 → Notion write

②·③에서 검출된 충돌의 수정 제안을 승인하면 backend가 target 블록을 Notion에 써넣고 충돌이 해소된다 (검출 출처와 무관).

- **승인** — `POST /api/conflicts/{conflictId}/findings/{findingId}/approve`, 본문 `{"expectedTargetNotionLastEditedAt": "{target 문서 notionLastEditedAt}"}` (값 불일치 시 409 stale). 인증은 `DG_SESSION` 쿠키.
- **결과** — target 블록이 `finding.newText`로 갱신(예: 설계서 `ND_COUNT=1로 정한다.` → `ND_COUNT=2로 정한다.`), conflict `resolved` → 인박스에서 빠짐. write는 bot author라 그로 인한 webhook은 무시된다(루프 없음).
