# DDD / CQRS 설계 원칙

## 패키지 구조

도메인 단위로 패키지를 나누고, 각 도메인 내부에서 Command/Query를 분리한다.

```
com.docgraph.backend/
└── {domain}/
    ├── command/
    │   ├── interfaces/
    │   │   ├── api/          # REST Controller, Web Request DTO
    │   │   └── event/        # Spring ApplicationEvent 리스너 (타 도메인 이벤트 수신)
    │   ├── application/      # Application Service, Command 객체
    │   ├── domain/           # Entity, Domain Service, Repository·외부 시스템 client 인터페이스, 도메인 이벤트
    │   └── infra/            # JPA Repository 구현체, 외부 시스템 어댑터
    └── query/
        ├── interfaces/
        │   └── api/          # REST Controller
        ├── application/      # Query Service, Response DTO
        └── infra/            # QueryDSL Repository (Response DTO를 프로젝션으로 반환)
```

`interfaces/`는 인바운드 진입점을 나타낸다. REST(`api/`)와 Spring Application Event(`event/`) 외에도 Batch, Scheduler 등이 추가될 수 있다.

## 도메인 간 통신

쓰기는 이벤트, 읽기는 Query API로 한다. 양쪽 모두 단방향 의존이며, 발행 도메인이 노출한 정의(이벤트 클래스, Query API 인터페이스)만 import한다.

### 쓰기 — 이벤트

- **이벤트 정의 위치**: 발행 도메인의 `command/domain/`
- **이벤트 이름**: `<Subject><PastAction>Event` (예: `ValidationPairCreatedEvent`)
- **리스너 위치**: 수신 도메인의 `interfaces/event/`
- **리스너 이름**: `<EventName>Listener` — 이벤트당 한 클래스 (예: `ValidationPairCreatedEventListener`). 같은 이벤트에 둘 이상의 처리가 필요하면 한 클래스 안에 메서드를 추가하지 말고, 처리 의도를 드러내는 접미사로 분리한다. 서로 다른 도메인이 같은 이벤트를 수신하면 수신 도메인을 클래스명 prefix로 붙여 구분한다 (예: `NotificationConflictDetectedEventListener`) — 빈 이름 충돌을 피하고 수신 도메인 의도를 드러낸다.
- **페이로드 원칙 — Thin Event**: 이벤트는 *식별자 + 이벤트 메타데이터(`occurredAt` 등)*만 담는다. **도메인 데이터는 페이로드에 박지 않는다** — 필요하면 consumer가 publisher의 Query API로 *처리 시점에* 조회한다. 발행 시점 스냅샷을 박으면 처리 지연 시 stale + 스키마 진화 부담↑ + "이 필드 stale 가능?" 분기 발생.

```
# 예시: graph → validation
graph/command/domain/ValidationPairCreatedEvent.kt                          # 이벤트
graph/command/application/GraphService.kt                                   # publishEvent() 호출
validation/command/interfaces/event/ValidationPairCreatedEventListener.kt   # @EventListener
```

```kotlin
// Thin Event 예시
data class ValidationPairCreatedEvent(
    val validationPairId: UUID,
    val edgeId: Long,                  // lookup handle — graph Query API로 추가 데이터 조회
    val occurredAt: OffsetDateTime,    // 이벤트 메타데이터
)
```

### 읽기 — Query API

읽기 연산은 **연산 단위로 분리**한다. 한 도메인이 여러 read 연산을 가지면 각각 별 인터페이스 + 별 핸들러로 둔다.

- **인터페이스 위치**: 발행 도메인의 `query/application/`
- **인터페이스 이름**: `<Verb><Target>(By<Criteria>)?Query` (Kotlin `fun interface`, 단일 메서드)
- **메서드 이름**: 인터페이스 이름의 Verb를 그대로 (`FindXQuery.find(...)`, `GetXQuery.get(...)`, `ListXQuery.list(...)`)
- **구현체 이름**: `<QueryName>Handler`
- **구현체 위치**: 발행 도메인의 `query/application/` (QueryDSL 등 영속 분리 필요 시 `query/infra/`로 별도 분리 가능)
- **호출 측**: HTTP Controller(`query/interfaces/api/`)와 cross-domain 호출자가 같은 인터페이스 공유

**Verb 의미 매핑** (연산 의미에 맞춰 선택):

| Verb | 사용 | 반환 |
| --- | --- | --- |
| `Get` | 존재 강제 (없으면 throw) | non-null |
| `Find` | nullable 단건 | nullable / Optional |
| `Search` | 조건 기반 필터 | List |
| `List` | 단순 컬렉션 | List |
| `Count` | 개수 | Long |
| `Exists` | 존재 여부 | Boolean |

```
# 예시: validation → document
document/query/application/FindDocumentByIdQuery.kt          # 인터페이스 (fun interface)
document/query/application/FindDocumentByIdQueryHandler.kt   # 구현체
validation/command/application/ValidationService.kt          # 의존성 주입
```

```kotlin
// FindDocumentByIdQuery.kt
fun interface FindDocumentByIdQuery {
    fun find(documentId: Long): DocumentDetail?
}

// FindDocumentByIdQueryHandler.kt
@Service
class FindDocumentByIdQueryHandler(...) : FindDocumentByIdQuery {
    override fun find(documentId: Long): DocumentDetail? = ...
}
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

## Command 컨벤션

쓰기 진입은 Command(데이터) + Handler(처리) 페어로 표현한다. 1 Command = 1 Handler.

- **Command 이름**: `<Verb><Target>Command` — 의도를 표현하는 동사. CRUD verb(`Create`/`Update`/`Delete`) 금지, 도메인 의도 verb(`Register`, `Approve`, `Assign`, `Submit`) 사용.
- **Handler 이름**: `<CommandName>Handler`
- **위치**: 둘 다 `<domain>/command/application/` (각각 별 파일)
- **메서드**: `handle(command): <Id 또는 Unit>`, `@Transactional`
- **도메인 이벤트 발행**: Handler 책임 (`ApplicationEventPublisher.publishEvent(...)`)

```kotlin
// command/application/RegisterUserCommand.kt
data class RegisterUserCommand(val email: String, val name: String)

// command/application/RegisterUserCommandHandler.kt
@Service
class RegisterUserCommandHandler(
    private val userRepository: UserRepository,
    private val publisher: ApplicationEventPublisher,
) {
    @Transactional
    fun handle(command: RegisterUserCommand): Long {
        val user = User.register(command.email, command.name)
        userRepository.save(user)
        publisher.publishEvent(UserRegisteredEvent(user.id))
        return user.id
    }
}
```

## DTO 배치 원칙

**소비하는 레이어에 함께 둔다.** 레이어 경계에서 변환이 일어나고, 각 레이어는 자신의 데이터 구조를 소유한다.

| 레이어 | DTO 종류 | 예시 파일 |
| --- | --- | --- |
| `command/interfaces/api/` | Web Request | `UserSignUpRequest.kt` |
| `command/application/` | Command 객체 | `UserSignUpCommand.kt` |
| `query/application/` | Query 객체 (있으면) | `UserSearchQuery.kt` |
| `query/application/` | Response DTO | `UserSummaryResponse.kt` |
