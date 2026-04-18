# 테스트 전략

## 레벨 구성

| 레벨 | 범위 | 도구 | 위치 |
| --- | --- | --- | --- |
| 단위 | 단일 메서드/클래스, 모든 의존성 Mock | JUnit 5, MockK | `apps/backend/` |
| 통합 | 실제 인프라와의 연동 (Repository/Service 레벨) | JUnit 5, Testcontainers | `apps/backend/` |
| 컴포넌트 | 모든 레이어 + 실제 인프라 + 외부 API Mock | JUnit 5, Testcontainers, WireMock, `@SpringBootTest` | `apps/backend/` |
| 시스템 | 전체 서비스를 실제로 띄운 상태에서 API 호출 | pytest, docker-compose | `tests/` |

## 커버리지 원칙

```
        [ 시스템 ]          ← Happy Path만
      [ 컴포넌트  ]         ← Happy Path 위주, 핵심 예외
    [   통합     ]          ← 인프라 연동 엣지케이스
  [     단위       ]        ← 모든 엣지케이스, 경계값, 예외 흐름
```

- **단위/통합**: 엣지케이스, 경계값, 실패 케이스를 최대한 이 레벨에서 소진한다.
- **컴포넌트**: 주요 흐름 위주. 레이어 통합 자체에서만 발생할 수 있는 예외는 추가한다.
- **시스템**: Happy Path 중심. 비즈니스 핵심 시나리오가 실제 환경에서 동작하는지 확인한다.

비용이 높은 상위 테스트에서 이미 하위에서 검증한 케이스를 반복하지 않는다.

## 레벨별 상세

### 단위 테스트

- 단일 메서드/클래스의 로직을 격리된 상태에서 검증
- 외부 의존성(DB, 외부 API 등)은 모두 MockK로 대체

### 통합 테스트

- Testcontainers가 PostgreSQL 컨테이너를 자동으로 띄워 실제 DB와 연동
- 특정 레이어(Repository, Service 등)가 인프라와 올바르게 연동되는지 검증

### 컴포넌트 테스트

- `@SpringBootTest`로 애플리케이션 전체를 구동
- 인프라는 Testcontainers로 실제 구동, 외부 API(Notion, AI API 등)는 WireMock으로 대체
- 단일 서비스 내 모든 레이어의 통합 동작 검증

### 시스템 테스트

- docker-compose로 백엔드를 실제로 띄운 상태에서 pytest로 API 직접 호출
- 서비스 전체의 비즈니스 흐름 검증
- `tests/`에서 `uv run pytest`로 실행

## 운영 원칙

- pytest는 머지 전에 반드시 통과해야 한다.
- 테스트 실패 시 테스트를 수정해서 통과시키지 않는다. 구현을 고친다. 단, 테스트 자체의 구조적 결함은 예외.
- 시스템 테스트(pytest)는 동작과 결과만 검증한다. API 스펙 세부사항(상태코드, 필드명, 응답 구조)은 Spring 컴포넌트 테스트에서 다룬다.

## 제외 항목

| 레벨 | 제외 이유 |
| --- | --- |
| 계약 테스트 (Pact) | 마이크로서비스 아키텍처가 아님 |
| E2E 테스트 (Playwright) | MVP 범위에서 제외 |
