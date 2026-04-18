# 개발 환경 가이드

## 사전 준비

다음이 설치되어 있어야 한다.

- Docker Desktop (또는 Docker Engine + Compose)
- Java 21
- Node.js 20+
- Python 3.12+ + [uv](https://docs.astral.sh/uv/getting-started/installation/)

`.env.example`을 복사해 `.env`를 만들고 값을 채운다.

```bash
cp .env.example .env
npm install   # JS 의존성 설치 (apps/frontend, packages/* 포함)
```

| 변수 | 설명 |
| --- | --- |
| `DB_PASSWORD` | 로컬 PostgreSQL 비밀번호 (임의 값) |
| `NGROK_AUTHTOKEN` | [ngrok 대시보드](https://dashboard.ngrok.com)에서 발급 |

---

## 로컬 인프라 구조

두 개의 compose 파일로 개발/테스트 환경을 분리한다.

**`docker-compose.yml`** — 기본 인프라

| 컨테이너 | 포트 | 용도 |
| --- | --- | --- |
| postgres | 5432 | 개발용 DB (데이터 영구 보존) |
| ngrok | 4040 | Notion Webhook을 로컬에서 수신하기 위한 터널 |
| backend | 8080 | 백엔드 컨테이너 (`--profile full` 시에만 실행) |

**`docker-compose.test.yml`** — 시스템 테스트 overlay

`uv run pytest` 실행 시 자동으로 적용된다. 직접 호출할 일은 없다.
`postgres-test`(tmpfs, 테스트마다 초기화)를 추가하고, 백엔드가 개발 DB 대신 이 DB에 연결되도록 교체해 개발 데이터가 오염되지 않도록 한다.

---

## 백엔드 개발

```bash
cd apps/backend
./gradlew bootRun   # docker compose(postgres + ngrok)를 자동으로 감지해 실행
./gradlew test      # 단위/통합/컴포넌트 테스트 (TestContainers로 DB 자동 구성)
```

### 시스템 테스트

```bash
cd tests
uv run pytest   # docker-compose.test.yml overlay 스택을 자동으로 띄우고, 완료 후 내림
```

### API 타입 생성

백엔드 OpenAPI 스펙으로부터 TypeScript 타입을 생성한다. 백엔드가 실행 중인 상태에서 실행한다.

```bash
npm run generate:types   # packages/api-types/src/schema.ts 갱신
```

컨트롤러가 추가되거나 DTO가 변경될 때마다 재실행한다.

---

## 프론트엔드 / 인프라 개발

백엔드를 직접 수정하지 않고 프론트엔드나 인프라 작업만 할 때는 백엔드를 컨테이너로 띄운다.

```bash
docker compose --profile full up   # postgres + ngrok + 백엔드 컨테이너
```

처음 실행 시 백엔드 이미지를 빌드하므로 시간이 소요된다.

백엔드가 뜨면 Swagger UI(`http://localhost:8080/swagger-ui.html`)에서 API 명세를 확인할 수 있다.
