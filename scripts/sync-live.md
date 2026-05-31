# sync-live — 실제 Notion 초기 동기화·충돌 검증 CLI (임시)

프론트 위저드·그래프 화면이 백엔드에 붙기 전까지, 실제 Notion 연동(동기화 → 엣지 → 충돌 검증)을 CLI로 돌려보는 임시 도구.

대상 스크립트: `scripts/sync-live.py` · 진입점: `just sync-live`

## 사전 준비

1. **백엔드 기동** — `just compose-up live` (ngrok 포함 — OAuth 로그인 도메인 제공).
2. **로그인 → 세션 쿠키**
   - 브라우저로 `https://<ngrok-domain>/api/oauth2/authorization/notion` 진입 → Notion 동의 화면에서 동기화할 페이지 선택·허용.
   - devtools → Application → Cookies → ngrok 도메인 → `DG_SESSION` 값 복사 (httpOnly라 콘솔엔 안 보임).
3. **환경변수** (`.env.local`에 넣거나 인라인 env로 전달 — 스크립트가 env에서 읽음):
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
