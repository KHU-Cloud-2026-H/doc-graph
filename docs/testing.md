# 테스트 전략

## 레벨 구성

| 레벨 | 범위 | 도구 | 위치 | 태그 |
| --- | --- | --- | --- | --- |
| 단위 | 단일 메서드/클래스, 모든 의존성 Mock | JUnit 5, MockK | `apps/backend/` | `@Tag("unit")` |
| 슬라이스 | 단일 레이어 + 그 레이어가 연동하는 인프라 | JUnit 5, Testcontainers, MockMvc | `apps/backend/` | `@Tag("slice")` |
| 컴포넌트 | 모든 레이어 + 실제 인프라 + 외부 API Mock | JUnit 5, Testcontainers, WireMock, `@SpringBootTest` | `apps/backend/` | `@Tag("component")` |
| 시스템 | 컨테이너 부팅·헬스·env wiring·인프라 결합 | pytest, Docker Compose | `tests/system/` | — |
| 인수 | UC happy path 시나리오 | pytest, Docker Compose, WireMock | `tests/acceptance/` | — |

## 커버리지 원칙

```
        [ 인수 ]           ← UC happy path, spec-first, 점진 통과
      [ 시스템  ]          ← 부팅·헬스·env wiring·인프라 결합
    [ 컴포넌트   ]         ← Happy Path + API 스펙 세부, 핵심 예외
  [   슬라이스    ]        ← 인프라 연동 엣지케이스
[     단위         ]      ← 모든 엣지케이스, 경계값, 예외 흐름
```

비용이 높은 상위 테스트에서 이미 하위에서 검증한 케이스를 반복하지 않는다.

## 명명 원칙 — 'test'는 포괄 의미일 때만

테스트 관련 명명에서 'test'는 여러 테스트 레벨을 동시에 포괄할 때만 사용한다.

| 포괄 의미 (OK) | 단일 의미 (다른 이름) |
| --- | --- |
| `application-test.yml` — unit·슬라이스·컴포넌트 공통 | acceptance 컨테이너 전용은 `application-acceptance.yaml` |
| `docker-compose.test.yml` — 시스템·인수 stack 공통 | (단일 레벨 compose 파일은 그 레벨 이름) |
| `tests/` 폴더 — 시스템·인수 | `tests/system/`·`tests/acceptance/`로 단일 레벨 분리 |
| `@Profile("test")` — JUnit 환경 묶음 | acceptance stack은 `@Profile("acceptance")` |

## 레벨별 상세

### 단위 테스트

- 단일 메서드/클래스의 로직을 격리된 상태에서 검증
- 외부 의존성(DB, 외부 API 등)은 모두 MockK로 대체

### 슬라이스 테스트

- Spring 슬라이스 어노테이션(`@DataJpaTest`, `@WebMvcTest` 등)으로 단일 레이어만 잘라 띄움
- 해당 레이어가 직접 연동하는 인프라(DB, MockMvc 등)만 실제 또는 동등한 도구. PostgreSQL은 Testcontainers

### 컴포넌트 테스트

- `@SpringBootTest`로 애플리케이션 전체를 구동
- 인프라는 Testcontainers로 실제 구동, 외부 API는 WireMock으로 대체
- 단일 서비스 내 모든 레이어의 통합 동작 + API 스펙 세부 검증

### 시스템 테스트

- docker-compose로 PostgreSQL · Backend를 띄우고 pytest는 외부 client로 application port에 호출
- 검증 대상: 컨테이너 부팅, application alive, Flyway 마이그레이션 적용, env wiring, 인프라 결합
- actuator는 외부 client 대상 아님 — management port 분리 유지, ALB target group 내부 검증

### 인수 테스트

- UC happy path 시나리오만 (`docs/usecases.md` 참고)
- 외부 의존성 stub은 mock/live 모드 가운데 선택 가능. 같은 코드가 양쪽에서 동작
- DB 격리는 backend `acceptance` profile의 reset endpoint로 처리. 운영 profile에선 endpoint 미등록

## 운영 원칙

- **시스템**: 머지 전 통과 필수.
- **인수**: spec-first / 점진 통과. 머지 전 회귀 금지. 미구현으로 인한 fail은 허용.
- 테스트 실패 시 테스트를 수정해서 통과시키지 않는다. 구현을 고친다. 단, 테스트 자체의 구조적 결함은 예외.

## 제외 항목

| 레벨 | 제외 이유 |
| --- | --- |
| 계약 테스트 (Pact) | 마이크로서비스 아키텍처가 아님 |
| E2E 테스트 (Playwright) | MVP 범위에서 제외 |