# 도메인 정의

## 개요

6개 도메인으로 구성한다. 전체 흐름은 두 축으로 나뉜다.

- **변경 감지**: Webhook 수신 → 문서 동기화 → 의존 관계 업데이트
- **정합성 검사**: 검증 대상 쌍 조회 → AI 검증 → 알림 발송

```
변경 감지
  Webhook 수신
  → document    문서 동기화, 타입 분류, 결정사항 추출
  → graph       의존 관계 엣지 생성/업데이트

정합성 검사
  graph         검증 대상 문서 쌍 조회 (read)
  → validation  AI 검증, 충돌 상태 관리, 충돌 해소
  → notification Notion 코멘트 삽입, Webhook 알림 발송
```

---

## auth

Notion OAuth 인증과 세션 관리를 담당한다. 별도 회원가입 플로우 없이 Notion OAuth가 계정 생성을 겸한다.

**핵심 개념**
- Notion OAuth 토큰 (암호화 저장)
- 세션

**경계**
- 워크스페이스 멤버십(초대, 역할)은 `workspace` 도메인 책임

---

## workspace

워크스페이스 등록과 팀원 관리를 담당한다.

**핵심 개념**
- 워크스페이스 (Notion 워크스페이스 연결 단위)
- 멤버 (Project Admin / Team Member)
- 워크스페이스 설정 (정합성 관리 활성화 여부, Webhook URL)

**경계**
- Notion API 호출은 `document` 도메인 책임

---

## document

Notion 문서의 동기화, 분류, 결정사항 추출을 담당한다. 변경 감지 흐름의 진입점이다.

**핵심 개념**
- Document (Notion 페이지 스냅샷, 타입: `meeting_notes` / `planning` / `requirements` / `design` / `research`)
- Decision (결정사항, 출처 문서 + 추출 시각)

**주요 흐름**
1. Notion Webhook 수신
2. Notion API로 변경된 페이지 전체 내용 조회
3. 폴더명·제목 키워드로 타입 분류 → 실패 시 AI API 호출
4. `meeting_notes`인 경우 AI API로 결정사항 추출

**경계**
- 의존 관계 생성은 `graph` 도메인 책임
- AI API 호출 결과를 소비하지만, AI 검증 로직은 `validation` 책임

---

## graph

문서 간 의존 관계를 모델링한다. 변경 감지에서 write, 정합성 검사에서 read로 양쪽 흐름에 관여하므로 독립 도메인으로 분리한다.

**핵심 개념**
- DependencyEdge (출발 문서 → 도착 문서, 체크 항목)
- Rule (의존 관계 자동 생성 규칙 테이블)

**주요 흐름**
- 문서 타입 결정 시 룰 테이블 기준으로 엣지 자동 생성
- 사용자 커스텀 엣지 추가/삭제
- 변경 문서와 연결된 엣지 목록 조회 → `validation`으로 전달

**기본 제공 룰**

| 출발 | 도착 | 체크 항목 |
| --- | --- | --- |
| `meeting_notes` | `planning` | 결정사항 반영 여부 |
| `meeting_notes` | `requirements` | 결정사항 반영 여부 |
| `research` | `planning` | 전제 조건 유지 여부 |
| `planning` | `requirements` | 범위 일치 여부 |
| `requirements` | `design` | 스펙 일치 여부 |

---

## validation

AI 기반 정합성 검증과 충돌 상태 관리를 담당한다.

**핵심 개념**
- ValidationRequest (검증 요청, 문서 쌍 + 체크 항목)
- ValidationResult (충돌 구간, 원인, 수정 제안)
- ConflictStatus (`detected` / `resolved`)

**주요 흐름**
1. `graph`로부터 검증 대상 문서 쌍 수신
2. AI API non-blocking 호출 (타임아웃 30초)
3. 결과 저장, `graph`의 엣지 상태 업데이트
4. 충돌 감지 시 `notification`으로 이벤트 발행
5. 사용자의 수동 해소 마킹 처리
6. AI 수정 제안 승인 시 Notion 문서에 직접 반영

**경계**
- 동일 이벤트 재처리 시 중복 검증 방지 (idempotency)
- 한 문서 쌍의 검증 실패가 다른 쌍에 영향을 주지 않아야 함 (실패 격리)

---

## notification

외부 알림 발송을 담당한다. `validation`으로부터 충돌 감지 이벤트를 수신한다.

**핵심 개념**
- NotificationChannel (워크스페이스별 Webhook URL)
- NotionComment (Notion 페이지 경고 코멘트)

**주요 흐름**
1. 충돌 감지 이벤트 수신
2. Notion 페이지에 경고 코멘트 삽입
3. 워크스페이스 Webhook URL로 알림 발송 (Slack·Discord 호환)

**경계**
- Webhook 알림·Notion 코멘트 삽입 실패 시에도 인앱 위반 표시는 `validation`이 유지
