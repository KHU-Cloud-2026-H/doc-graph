# DDD / CQRS 설계 원칙

## 패키지 구조

도메인 단위로 패키지를 나누고, 각 도메인 내부에서 Command/Query를 분리한다.

```
com.docgraph/
└── {domain}/
    ├── command/
    │   ├── interfaces/
    │   │   ├── api/          # REST Controller, Web Request DTO
    │   │   └── event/        # Spring ApplicationEvent 리스너 (타 도메인 이벤트 수신)
    │   ├── application/      # Application Service, Command 객체
    │   ├── domain/           # Entity, Domain Service, Repository 인터페이스, 도메인 이벤트
    │   └── infra/            # JPA Repository 구현체
    └── query/
        ├── interfaces/
        │   └── api/          # REST Controller
        ├── application/      # Query Service, Response DTO
        └── infra/            # QueryDSL Repository (Response DTO를 프로젝션으로 반환)
```

`interfaces/`는 인바운드 진입점을 나타낸다. REST(`api/`)와 Spring Application Event(`event/`) 외에도 Batch, Scheduler 등이 추가될 수 있다.

도메인 이벤트는 **발행하는 도메인의 `command/domain/`** 에 정의한다. 수신하는 도메인은 `interfaces/event/` 리스너에서 해당 이벤트 클래스를 참조한다 (단방향 의존).

```
# 예시: graph → validation 이벤트
graph/command/domain/ValidationRequiredEvent.kt      # 이벤트 정의
graph/command/application/GraphService.kt            # publishEvent() 호출
validation/command/interfaces/event/ValidationEventListener.kt  # @EventListener
```

## Command / Query 분리 원칙

| | Command | Query |
| --- | --- | --- |
| 목적 | 상태 변경 | 데이터 조회 |
| Entity 사용 | O | X (DTO로 직접 프로젝션) |
| Repository | JPA (`infra/`) | QueryDSL (`infra/`) |
| Response | id 정도만 반환 | Response DTO (`application/`에 정의) |

## 레이어별 책임

### interfaces/api
- HTTP 요청/응답 변환
- 비즈니스 로직 없음
- Application Service 호출

### application
- 유스케이스 오케스트레이션
- 트랜잭션 경계
- Domain Service, Repository 호출
- **비즈니스 로직 없음** — 로직은 Domain으로 위임

### domain (Command only)
- **Entity**: 상태와 비즈니스 규칙을 캡슐화
- **Domain Service**: 단일 Entity에 속하지 않는 도메인 로직
- **Repository 인터페이스**: 구현체는 infra에 위치

### infra
- Command: JPA Repository 구현체
- Query: QueryDSL Repository — `application/`에 정의된 Response DTO를 프로젝션으로 반환

## DTO 배치 원칙

**소비하는 레이어에 함께 둔다.** 레이어 경계에서 변환이 일어나고, 각 레이어는 자신의 데이터 구조를 소유한다.

| 레이어 | DTO 종류 | 예시 파일 |
| --- | --- | --- |
| `command/interfaces/api/` | Web Request | `UserRequests.kt` |
| `command/application/` | Command 객체 | `UserCommands.kt` |
| `query/application/` | Response DTO | `UserResponses.kt` |

**파일 구성**: 관련 DTO는 하나의 파일로 묶는다. Kotlin은 파일 하나에 여러 top-level 클래스를 둘 수 있으므로, 중첩 클래스 없이 파일 단위로 그룹핑하는 것이 관용적이다.

```kotlin
// UserRequests.kt (command/interfaces/api/)
data class UserSignUpRequest(val email: String, val name: String)

// UserCommands.kt (command/application/)
data class UserSignUpCommand(val email: String, val name: String)

// UserResponses.kt (query/application/)
data class UserSummaryResponse(val id: Long, val name: String)
```
