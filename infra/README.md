# infra

## 2026-06-05 update: API Gateway + private S3 frontend

The backend-only ALB entrypoint has been extended to a single HTTPS entrypoint
through API Gateway.

Current traffic flow:

```text
User / Notion
  -> HTTPS API Gateway HTTP API ($default stage)
     -> /api/*: VPC Link -> ALB HTTP:80 -> ECS Fargate Spring Boot -> RDS
     -> /assets/* and SPA fallback: REST API S3 proxy -> private S3 React build bucket
```

Important constraints reflected in the Terraform:

- CloudFront, Route 53, ACM, and ALB HTTPS listeners are still not used.
- `sg-alb` no longer accepts internet ingress directly. HTTP:80 is allowed from
  the API Gateway VPC Link security group only.
- The React build bucket blocks all public access. Direct S3 object URLs should
  return AccessDenied.
- AWS HTTP API(v2) does not currently support S3 `GetObject` as a first-class
  AWS service integration. To keep the public endpoint as an HTTP API while
  serving private S3 objects, `modules/apigateway` creates a narrow REST API(v1)
  S3 proxy and routes frontend requests to it from the HTTP API.
- API Gateway uses the existing Learner Lab `LabRole` ARN for the S3 integration;
  no IAM roles or policies are created by Terraform.

New modules and scripts:

- `modules/s3-frontend`: private S3 bucket for `apps/frontend/dist`.
- `modules/apigateway`: HTTP API, VPC Link, `/api/{proxy+}` ALB route, and S3
  frontend proxy wiring.
- `scripts/push-frontend.sh`: builds `apps/frontend` and syncs `dist` to S3.

New or changed outputs:

- `api_gateway_endpoint`: main HTTPS URL for the app, Notion OAuth redirect, and
  webhook registration.
- `swagger_ui_url`: now uses `api_gateway_endpoint`.
- `frontend_bucket_name`: target bucket for frontend deployment.

Frontend deployment is included in `scripts/deploy.sh` after `terraform apply`.
For frontend-only redeploys:

```bash
cd infra
./scripts/push-frontend.sh my-lab
```

DocGraph AWS 인프라 (Terraform). AWS Learner Lab 환경에서 운영합니다.

## 현재 상태

| 단계 | 리소스 | 상태 |
|------|--------|------|
| 1단계 | VPC / 서브넷 / IGW | ✅ |
| 2단계 | ECR | ✅ |
| 3단계 | ALB + ECS Fargate | ✅ |
| 4단계 | RDS PostgreSQL 17 | ✅ |
| 5단계 | Secrets Manager | ✅ |

## 아키텍처

### 트래픽 흐름

ALB를 단일 퍼블릭 엔드포인트로 사용합니다. Notion Webhook URL과 OAuth Redirect URI 모두 ALB DNS를 등록합니다.

### 네트워크 토폴로지

```
                                 Internet
                                    │
                       ┌────────────▼──────────────┐
                       │      ALB  (main-alb)      │
                       │   HTTP :80                │
                       └────────────┬──────────────┘
                                    │ :8080
╔════════════════════════════════╪══════════════════════════════════════════╗
║  VPC  10.0.0.0/16   us-east-1                                             ║
║                                                                           ║
║  ┌── Public Subnet (us-east-1a 10.0.1.0/24, us-east-1b 10.0.2.0/24) ────┐ ║
║  │                                                                      │ ║
║  │          ┌────────────────────────────────────────┐                  │ ║
║  │          │          ECS Fargate  (app-task)       │                  │ ║
║  │          │      1 vCPU · 2 GB · port 8080         │                  │ ║
║  │          │      assign_public_ip = true           │                  │ ║
║  │          └────────────────────┬───────────────────┘                  │ ║
║  └───────────────────────────────┼──────────────────────────────────────┘ ║
║                                  │ :5432                                  ║
║  ┌── Private Subnet (us-east-1a 10.0.11.0/24, us-east-1b 10.0.12.0/24) ─┐ ║
║  │                                                                      │ ║
║  │          ┌────────────────────────────────────────┐                  │ ║
║  │          │          RDS PostgreSQL 17             │                  │ ║
║  │          │      db.t3.micro · 20 GB               │                  │ ║
║  │          │      publicly_accessible = false       │                  │ ║
║  │          └────────────────────────────────────────┘                  │ ║
║  └──────────────────────────────────────────────────────────────────────┘ ║
║                                                                           ║
║  IGW ──→ Public Route Table (0.0.0.0/0 → IGW)                             ║
║          Private Route Table (local only)                                 ║
╚═══════════════════════════════════════════════════════════════════════════╝
              │                               │
   ECR  (public endpoint)        Secrets Manager  (AWS managed)
   Fargate 공인 IP로 직접 pull    ECS 태스크 기동 시 시크릿 주입
   NAT Gateway 불필요             (rds-password, notion-*, openai-*)
```

### VPC / 서브넷

| 구분 | 이름 | CIDR | AZ |
|------|------|------|----|
| VPC | main-vpc | `10.0.0.0/16` | — |
| Public | public-us-east-1a | `10.0.1.0/24` | us-east-1a |
| Public | public-us-east-1b | `10.0.2.0/24` | us-east-1b |
| Private | private-us-east-1a | `10.0.11.0/24` | us-east-1a |
| Private | private-us-east-1b | `10.0.12.0/24` | us-east-1b |

### Security Group

| SG | Inbound | Outbound |
|----|---------|----------|
| sg-alb | `0.0.0.0/0` → :80, :443 | all |
| sg-fargate | sg-alb → :8080 | all (ECR pull · Secrets Manager · 외부 API) |
| sg-rds | sg-fargate → :5432 | all |

### 리소스 사양

| 리소스 | 구성 |
|--------|------|
| **ALB** | external · HTTP :80 → Target Group HTTP :8080 (IP type) |
| **ALB Health Check** | `GET /actuator/health` · 200 · interval 30s · timeout 10s · healthy 2 · unhealthy 5 |
| **ECS Cluster** | app-cluster |
| **ECS Task** | Fargate · 1 vCPU · 2 GB · IAM: LabRole |
| **ECS Service** | desired 1 · grace period 180s · circuit breaker on (rollback off) |
| **Container** | `MANAGEMENT_SERVER_PORT=8080` · `MaxRAMPercentage=75%` · startPeriod 120s · 민감 값은 Secrets Manager 참조 |
| **Secrets Manager** | `docgraph/rds-password`, `docgraph/notion-client-id`, `docgraph/notion-client-secret`, `docgraph/openai-api-key` |
| **RDS** | PostgreSQL 17 · db.t3.micro · 20 GB · Multi-AZ off · private subnet |
| **ECR** | spring-boot-app · MUTABLE · 최대 5개 이미지 보관 |
| **CloudWatch Logs** | `/ecs/app` · 보존 7일 |

## 사전 요구사항

- [Terraform](https://developer.hashicorp.com/terraform/install) >= 1.6
- [AWS CLI](https://aws.amazon.com/cli/) v2
- [Docker Desktop](https://www.docker.com/products/docker-desktop/)
- AWS Learner Lab 계정 (IAM은 `LabRole`만 사용)

## 환경 설정

### 1. Learner Lab 자격증명 등록

Learner Lab 콘솔 → **AWS Details** → **AWS CLI** 에서 발급한 키를 입력합니다.

```bash
./scripts/update_credentials.sh my-lab
```

> 자격증명은 3~4시간마다 만료됩니다. 만료 시 이 명령을 다시 실행하세요.

### 2. `terraform.tfvars` 작성

`terraform.tfvars`는 `.gitignore`에 포함되어 커밋되지 않습니다. Terraform이 이 값을 읽어 Secrets Manager에 저장하고, ECS는 태스크 기동 시 Secrets Manager에서 직접 주입받습니다.

```hcl
# infra/terraform.tfvars

aws_profile  = "my-lab"
rds_password = "변경할_DB_비밀번호"

notion_client_id     = "Notion_OAuth_Client_ID"
notion_client_secret = "Notion_OAuth_Client_Secret"

ai_openai_api_key  = "sk-..."
ai_openai_base_url = "https://api.openai.com"
ai_openai_model    = "gpt-4o"
```

Notion OAuth 앱은 [Notion 개발자 콘솔](https://www.notion.so/my-integrations)에서 생성합니다.
Redirect URI와 Webhook URL에 사용할 ALB DNS는 `terraform apply` 후 출력됩니다.

```
http://<alb-dns>/api/login/oauth2/code/notion   # OAuth Redirect URI
http://<alb-dns>/api/...                        # Notion Webhook URL
```

## 배포

### 최초 배포 / 코드 수정 재배포 (공통)

```bash
cd infra
./scripts/deploy.sh
```

내부 실행 순서:

1. 자격증명 유효성 확인
2. ECR 리포지터리 생성 (`-target=module.ecr`, 이미 있으면 no-op)
3. Docker 이미지 빌드 & ECR Push — **첫 빌드는 5~10분 소요**
4. 나머지 인프라 전체 apply — **RDS 최초 생성 시 10~15분 추가 소요**
5. ECS 서비스 강제 재배포
6. Swagger UI URL 출력

ECS 태스크가 ALB health check를 통과할 때까지 **배포 완료 후 약 3~5분** 대기합니다.

### 이미지만 재빌드할 때

```bash
./scripts/push-image.sh

aws ecs update-service \
  --cluster app-cluster \
  --service app-service \
  --force-new-deployment \
  --profile my-lab --region us-east-1
```

### 상태 확인

```bash
# ECS 배포 상태
aws ecs describe-services \
  --cluster app-cluster --services app-service \
  --profile my-lab --region us-east-1 \
  --query 'services[0].deployments[*].{status:status,running:runningCount,desired:desiredCount}'

# Spring Boot 기동 로그
aws logs tail /ecs/app --follow --profile my-lab --region us-east-1

# Swagger UI URL (ALB 직접)
terraform output swagger_ui_url
```

### 리소스 정리

```bash
terraform destroy
```

## 모듈 구성

```
infra/
├── modules/
│   ├── vpc/      VPC, 퍼블릭·프라이빗 서브넷, IGW, 라우팅 테이블
│   ├── sg/       Security Group 3개 — ALB / ECS Fargate / RDS
│   ├── ecr/      ECR 리포지터리 (이미지 최대 5개 보관)
│   ├── alb/      ALB, Target Group, HTTP 리스너
│   ├── ecs/      ECS Cluster, Task Definition, Fargate Service
│   ├── rds/      RDS PostgreSQL 17 (db.t3.micro, 프라이빗 서브넷)
│   ├── secrets/  Secrets Manager 시크릿 4개 (rds-password, notion-*, openai-*)
├── scripts/
│   ├── deploy.sh               최초·재배포 통합 스크립트
│   ├── push-image.sh           ECR 이미지 빌드 & Push
│   └── update_credentials.sh  Learner Lab 자격증명 갱신
├── main.tf
├── variables.tf
├── outputs.tf
├── provider.tf
└── versions.tf
```

## 주요 설계 결정

- **ALB 단일 엔드포인트**: Notion Webhook과 OAuth Redirect URI 모두 ALB DNS로 등록. 프론트엔드 아키텍처 확정 전까지 CDN 레이어 없이 운영.
- **Secrets Manager**: 민감 값(RDS 비밀번호, Notion OAuth, OpenAI API 키)을 Secrets Manager에 저장. ECS 태스크 정의에는 ARN만 기록되고, 컨테이너 기동 시 LabRole이 값을 직접 주입.
- **ECS Public Subnet + 공인 IP**: NAT Gateway 없이 ECR 이미지 pull 및 외부 API 호출. sg-fargate가 ALB inbound만 허용하여 직접 접근 차단. Learner Lab 크레딧 절약.
- **Actuator 포트 통합**: `MANAGEMENT_SERVER_PORT=8080`으로 ALB health check가 `/actuator/health`에 접근.
- **`health_check_grace_period_seconds = 180`**: Spring Boot + Flyway 기동 시간 확보. 미설정 시 기동 전 태스크가 교체되는 무한 루프 발생.
- **`recovery_window_in_days = 0`**: Secrets Manager 시크릿을 `terraform destroy` 시 즉시 삭제. 동일 이름으로 재배포 시 복구 기간 충돌 방지.

## 향후 계획

- [ ] Notion OAuth 실 자격증명 적용 및 인증 플로우 검증
- [ ] AI 검증 파이프라인 (ValidationTask → OpenAI) 통합 테스트
- [ ] (P1) 연결 제안 기능, 인박스 구현
- [ ] (P2) Slack / Discord Webhook 알림
- [ ] HTTPS 전환 (ACM 인증서 + ALB HTTPS 리스너)
