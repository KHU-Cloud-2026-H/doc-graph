# DocGraph 아키텍처 설계

## 컴퓨팅 모델

Spring Boot on ECS/Fargate가 모든 서버 로직을 담당한다. Webhook 수신, API 서버, AI API 호출(WebClient non-blocking)을 단일 애플리케이션에서 처리한다.

| 영역 | 성격 | 처리 방식 |
| --- | --- | --- |
| API 서버 (그래프 조회, OAuth, 충돌 해소) | 상시 실행, 복잡한 비즈니스 로직 | Spring Boot on ECS/Fargate |
| 변경 감지 (Webhook 수신) | 이벤트 기반, 빠른 응답 필요 | Spring Boot — Webhook 수신 → 의존 쌍 조회 → 즉시 200 반환 |
| 정합성 검증 (AI 호출) | 느린 비동기 처리 | Spring Boot — WebClient non-blocking으로 AI API 호출, 스레드 점유 없음 |

## 전체 아키텍처

```text
[사용자]
    ↓
CloudFront + S3           ← 프론트 호스팅
    ↓
ALB (Application Load Balancer)
    ↓
Spring Boot on ECS/Fargate  ← 모든 서버 로직
  - Notion OAuth 연동
  - 의존 관계 그래프 CRUD API
  - 충돌 해소 API
  - 결정사항 조회 API
  - Notion Webhook 수신 엔드포인트
    → 의존 관계 쌍 조회 (RDS)
    → WebClient로 AI API non-blocking 호출
    → 즉시 200 반환
  - AI 검증 결과 처리
    → 결과 RDS 저장
    → Notion 코멘트 자동 삽입
    → Webhook 알림 발송 (Slack/Discord 등)
    ↓
RDS PostgreSQL            ← 그래프 구조, 충돌 이력, 문서 메타데이터 통합
```

## 기술 스택

| 레이어 | 기술 | 비고 |
| --- | --- | --- |
| 프론트엔드 | React | S3 + CloudFront 배포 |
| API 서버 + Webhook + AI 호출 | Kotlin, Spring Boot | Docker 컨테이너화, ECS/Fargate 배포. AI API는 WebClient non-blocking 호출 |
| DB | RDS PostgreSQL | 그래프 구조, 충돌 이력, 문서 메타데이터 통합 |
| 문서 소스 | Notion API (OAuth) + Webhook | 변경 시 실시간 이벤트 수신 |
| AI 호출 | 외부 AI API | WebClient로 Spring Boot 내에서 non-blocking 호출 |
| 알림 | 인앱 표시 + Webhook (Slack/Discord 등) | Webhook URL 프로젝트별 설정 |
| 프론트 호스팅 | S3 + CloudFront | |
| 컨테이너 오케스트레이션 | ECS/Fargate | EC2 관리 불필요 |
| 자격증명 관리 | Secrets Manager | DB 비밀번호, AI API 키, OAuth 토큰 |
| 컨테이너 레지스트리 | ECR | Spring Boot Docker 이미지 저장 |

## 개발 환경

인프라가 Terraform으로 AWS 환경을 구성하고, 백엔드가 로컬에서 E2E 통과한 코드를 인프라가 Learner Lab에 배포하는 방식으로 역할을 분리한다.

**로컬 개발 환경:** `docker-compose.yml` 하나로 로컬 개발과 시스템 테스트를 모두 커버한다.

- 백엔드 개발: `docker compose up` (PostgreSQL + ngrok) + IDE에서 Spring Boot 직접 실행
- 프론트/인프라 개발: `docker compose --profile full up` (PostgreSQL + ngrok + Spring Boot 컨테이너)
- 시스템 테스트: `docker compose --profile full up`. 테스트 전용 DB(postgres-test, tmpfs)를 별도 컨테이너로 분리해 개발 데이터를 보존한다.

| 컨테이너 | 포트 | 용도 | 스토리지 |
| --- | --- | --- | --- |
| postgres | 5432 | 로컬 개발 | named volume (데이터 유지) |
| postgres-test | 5433 | 시스템 테스트 전용 | tmpfs (테스트마다 초기화) |

```text
백엔드
  Spring Boot (Webhook 수신 + API + AI 호출) 로직 작성
  docker-compose (PostgreSQL + ngrok)에서 시스템 테스트
      ↓
인프라 담당
  시스템 테스트 통과한 코드를 Learner Lab에 배포 (Terraform apply + 수동 배포)
```

## 미확정 사항

- AI API 공급자 (OpenAI / Claude API / 기타)
- 프론트엔드 그래프 시각화 라이브러리 (D3.js / React Flow 등)
