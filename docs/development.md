# 개발 환경 가이드

## 사전 준비

공통:

- Docker Desktop (또는 Docker Engine + Compose)
- [just](https://just.systems/) — dev 명령 진입점
- [dotenvx](https://dotenvx.com/docs/install) — 환경변수 주입·암호화
- (Windows 전용) Git for Windows — `C:\Program Files\Git\usr\bin`이 PATH에 포함되어야 한다

백엔드:

- Java 21
- (시스템 테스트) [uv](https://docs.astral.sh/uv/getting-started/installation/)

프론트엔드:

- Node.js 24+

---

## 환경변수

| 파일 | 추적 | 역할 |
| --- | --- | --- |
| `.env` | commit | 평문 default + 팀 공유 시크릿 (dotenvx 암호화) |
| `.env.local` | gitignored | 개인 시크릿 |
| `.env.keys` | gitignored | dotenvx 복호화 키 |

1. `.env.keys` 수령
2. `.env`의 빈 placeholder 항목을 `.env.local`에 채움

---

## 로컬 인프라 구조

`docker-compose.yml` — 기본 인프라

| 컨테이너 | 호스트 포트 | 용도 |
| --- | --- | --- |
| postgres | 5433 | 개발용 DB (데이터 영구 보존) |
| backend | 8080 | 백엔드 컨테이너 (`just compose-up` 시에만 실행, acceptance profile) |
| ngrok | 4040 | Notion Webhook 로컬 수신 터널 (`live` mode 한정) |
| wiremock | env | 외부 API stub (`mock` mode 한정) |

---

## 백엔드 개발

```bash
just bootRun                  # postgres + ngrok 자동 기동 후 백엔드 실행
just test-unit                # 단위
just test-slice               # 슬라이스
just test-component           # 컴포넌트
just test-all                 # 전체
just test-class [ClassName]   # 단일 클래스
```

### 시스템·인수 테스트

```bash
just test-system                  # 부팅·env wiring (로컬 compose 자동)
just test-acceptance              # UC happy path · local-mock (default)
just test-acceptance local-live   # 로컬 backend + 실제 외부 API
just test-acceptance remote-live  # ECS endpoint + 실제 외부 API
```

원격 모드 사용 전 `.env.local`에 `ACCEPTANCE_REMOTE_BACKEND_URL`을 채운다.

### API 스펙 산출물

```bash
just openapi-dump   # packages/api-types/openapi.json 추출
just gen-types      # packages/api-types/src/schema.ts 생성 (openapi-dump 포함)
just gen-redoc      # apps/docs/dist/index.html Redoc 빌드 (openapi-dump 포함)
```

---

## 프론트엔드 / 인프라 개발

백엔드를 직접 수정하지 않고 프론트엔드나 인프라 작업만 할 때는 백엔드를 컨테이너로 띄운다.

```bash
just compose-up            # postgres + backend + wiremock (default mock)
just compose-up live       # mock 대신 ngrok + 실제 외부 API
just compose-down
```

mock 모드는 외부 API(Notion·OpenAI)를 wiremock으로 stub한다. live 모드는 실제 호출.

처음 실행 시 백엔드 이미지를 빌드하므로 시간이 소요된다.

백엔드가 뜨면 Swagger UI(`http://localhost:8080/api/swagger-ui.html`)에서 API 명세를 확인할 수 있다.