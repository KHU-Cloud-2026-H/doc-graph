# DocGraph Infra

DocGraph의 AWS 인프라를 Terraform으로 관리하는 디렉터리입니다. 현재 구성은 AWS Learner Lab 환경을 기준으로 하며, 퍼블릭 진입점은 API Gateway HTTPS 엔드포인트 하나로 통일되어 있습니다.

## 전체 구조

```text
사용자 / Notion
  -> API Gateway HTTP API ($default, HTTPS)
     -> /api/*   -> VPC Link -> 내부 ALB HTTP:80 -> ECS Fargate(Spring Boot) -> RDS PostgreSQL
     -> /assets/*, SPA fallback -> REST API S3 proxy -> Private S3 React build bucket
```

주요 제약과 설계 방향은 다음과 같습니다.

- CloudFront, Route 53, ACM, ALB HTTPS Listener는 아직 사용하지 않습니다.
- ALB는 `internal = true`이며 인터넷에서 직접 접근하지 않습니다.
- `sg-alb`는 API Gateway VPC Link 보안 그룹에서 들어오는 HTTP 80만 허용합니다.
- 프론트엔드 S3 버킷은 Public Access Block이 적용된 private bucket입니다. 직접 S3 Object URL 접근은 막혀야 합니다.
- API Gateway HTTP API(v2)는 S3 `GetObject`를 직접 AWS Service Integration으로 다루기 어렵기 때문에, `modules/apigateway`에서 좁은 범위의 REST API(v1) S3 proxy를 만들고 HTTP API가 이를 프록시합니다.
- Learner Lab의 기존 `LabRole`을 ECS Task Role, Execution Role, API Gateway S3 Integration Role로 사용합니다. Terraform은 별도 IAM Role/Policy를 만들지 않습니다.

## 리소스 요약

| 구분 | 구성 |
| --- | --- |
| VPC | `main-vpc`, `10.0.0.0/16`, DNS hostnames/support 활성화 |
| Public Subnet | `10.0.1.0/24`, `10.0.2.0/24`, Fargate public IP 할당 |
| Private Subnet | `10.0.11.0/24`, `10.0.12.0/24`, RDS 및 API Gateway VPC Link/내부 ALB 용도 |
| API Gateway | HTTP API `$default`, `/api/{proxy+}`, `/assets/{proxy+}`, SPA fallback |
| ALB | Internal Application Load Balancer, HTTP 80 Listener, Target Group 8080 |
| ECS | Fargate 1 vCPU / 2 GB, desired count 1, container port 8080 |
| RDS | PostgreSQL 17, `db.t3.micro`, 20 GB, private subnet, public access 비활성화 |
| ECR | `spring-boot-app`, mutable tag, 최근 이미지 5개 유지 |
| S3 Frontend | `docgraph-frontend-*` private bucket, React `dist` 업로드 대상 |
| Secrets Manager | RDS 비밀번호, Notion OAuth, OpenAI API key 저장 |
| CloudWatch Logs | `/ecs/app`, 7일 보관 |

## 네트워크와 보안 그룹

| 보안 그룹 | Inbound | Outbound |
| --- | --- | --- |
| `sg-apigw-vpc-link` | 없음 | 전체 허용 |
| `sg-alb` | `sg-apigw-vpc-link` -> TCP 80 | 전체 허용 |
| `sg-fargate` | `sg-alb` -> TCP 8080 | 전체 허용 |
| `sg-rds` | `sg-fargate` -> TCP 5432 | 전체 허용 |

Fargate Task는 Public Subnet에서 `assign_public_ip = true`로 실행됩니다. Learner Lab 비용/제약을 고려해 NAT Gateway 없이 ECR pull, Secrets Manager 접근, 외부 API 호출을 처리하기 위한 구성입니다. 외부에서 Task로 직접 들어오는 트래픽은 보안 그룹에서 허용하지 않습니다.

## 디렉터리 구성

```text
infra/
  modules/
    apigateway/   API Gateway HTTP API, VPC Link, S3 proxy REST API
    alb/          내부 ALB, Target Group, HTTP Listener
    ecr/          Spring Boot Docker 이미지 저장소
    ecs/          ECS Cluster, Task Definition, Fargate Service
    rds/          PostgreSQL RDS
    s3-frontend/  React build artifact용 private S3 bucket
    secrets/      Secrets Manager secret/version
    sg/           API Gateway, ALB, Fargate, RDS 보안 그룹
    vpc/          VPC, Subnet, IGW, Route Table
  scripts/
    deploy.sh              전체 배포
    push-image.sh          백엔드 Docker build 및 ECR push
    push-frontend.sh       프론트엔드 build 및 S3 sync
    update_credentials.sh  Learner Lab AWS credential 갱신
  main.tf
  variables.tf
  outputs.tf
  provider.tf
  versions.tf
```

## 사전 준비

- Terraform `>= 1.6`
- AWS CLI v2
- Docker Desktop
- Node.js/npm
- Bash 실행 환경: macOS/Linux/WSL/Git Bash 등
- AWS Learner Lab 계정 및 `LabRole`

## 환경 설정

### 1. Learner Lab Credential 등록

Learner Lab 콘솔의 **AWS Details > AWS CLI**에서 발급받은 값을 입력합니다.

```bash
cd infra
./scripts/update_credentials.sh my-lab
```

Credential은 몇 시간 단위로 만료됩니다. AWS CLI 또는 Terraform에서 인증 오류가 나면 같은 명령으로 다시 갱신합니다.

### 2. terraform.tfvars 작성

`terraform.tfvars`는 `.gitignore`에 포함되어 커밋되지 않습니다. 민감 값은 Terraform apply 시 Secrets Manager에 저장되고, ECS Task 실행 시 secret ARN을 통해 주입됩니다.

```hcl
aws_profile = "my-lab"
aws_region  = "us-east-1"

rds_password = "변경할_DB_비밀번호"

notion_client_id     = "Notion_OAuth_Client_ID"
notion_client_secret = "Notion_OAuth_Client_Secret"

ai_openai_api_key  = "sk-..."
ai_openai_base_url = "https://api.openai.com"
ai_openai_model    = "gpt-4o"
```

## 배포

### 전체 배포

```bash
cd infra
./scripts/deploy.sh my-lab
```

스크립트 실행 흐름은 다음과 같습니다.

1. AWS credential 유효성 확인
2. ECR repository 선생성: `terraform apply -target=module.ecr`
3. `apps/backend` Docker image build 및 ECR push
4. 전체 Terraform apply
5. `apps/frontend` build 및 private S3 bucket sync
6. ECS Service 강제 재배포
7. App URL, Swagger UI URL, ALB DNS 출력

최초 배포는 Docker/Gradle 의존성 다운로드와 RDS 생성 때문에 오래 걸릴 수 있습니다. RDS 생성은 보통 10분 이상 걸릴 수 있고, ECS Task가 ALB health check를 통과하기까지 추가로 몇 분이 필요합니다.

### 백엔드 이미지만 다시 배포

```bash
cd infra
./scripts/push-image.sh my-lab

aws ecs update-service \
  --cluster app-cluster \
  --service app-service \
  --force-new-deployment \
  --profile my-lab \
  --region us-east-1
```

### 프론트엔드만 다시 배포

```bash
cd infra
./scripts/push-frontend.sh my-lab
```

`push-frontend.sh`는 `apps/frontend`에서 build를 실행한 뒤, `dist` 결과물을 `frontend_bucket_name` 출력값으로 확인한 S3 bucket에 업로드합니다. `index.html`은 `no-cache`, 그 외 asset은 장기 cache header로 업로드합니다.

## 접속 정보

배포 후 Terraform output으로 주요 URL을 확인합니다.

```bash
terraform output api_gateway_endpoint
terraform output swagger_ui_url
terraform output frontend_bucket_name
terraform output ecr_repository_url
```

용도별 URL은 다음과 같습니다.

| 용도 | URL |
| --- | --- |
| 앱 접속 | `api_gateway_endpoint` |
| Swagger UI | `${api_gateway_endpoint}/api/swagger-ui/index.html` |
| Notion OAuth Redirect URI | `${api_gateway_endpoint}/api/login/oauth2/code/notion` |
| API 호출 | `${api_gateway_endpoint}/api/...` |

## 상태 확인

```bash
# ECS 배포 상태
aws ecs describe-services \
  --cluster app-cluster \
  --services app-service \
  --profile my-lab \
  --region us-east-1 \
  --query 'services[0].deployments[*].{status:status,running:runningCount,desired:desiredCount}'

# Spring Boot 로그
aws logs tail /ecs/app --follow --profile my-lab --region us-east-1

# Swagger UI URL
terraform output swagger_ui_url
```

ALB Target Group health check 경로는 `/api/actuator/health`입니다. ECS container health check는 `http://localhost:8080/actuator/health`를 사용합니다.

## 정리

```bash
cd infra
terraform destroy
```

Secrets Manager 리소스는 `recovery_window_in_days = 0`으로 설정되어 있어 destroy 시 즉시 삭제됩니다. Learner Lab에서 같은 secret 이름으로 재배포할 때 복구 대기 기간 충돌을 피하기 위한 설정입니다.

## 주요 출력값

| Output | 설명 |
| --- | --- |
| `api_gateway_endpoint` | 앱/API/Notion 연동에 사용하는 메인 HTTPS 엔드포인트 |
| `swagger_ui_url` | Swagger UI 접속 주소 |
| `alb_dns_name` | 내부 ALB DNS 이름 |
| `frontend_bucket_name` | React build artifact 업로드 대상 S3 bucket |
| `ecr_repository_url` | Docker image push 대상 ECR URL |
| `rds_endpoint` | RDS endpoint, sensitive output |

## 운영 메모

- 현재 운영 기준 진입점은 ALB가 아니라 API Gateway입니다.
- S3 frontend bucket은 private 상태가 정상입니다. 퍼블릭 S3 URL로 접근되지 않아야 합니다.
- API Gateway HTTP API가 `/api/*`는 백엔드로, `/assets/*`와 그 외 SPA route는 S3 proxy로 보냅니다.
- `app_image_tag` 기본값은 `latest`입니다. `push-image.sh`도 `latest` tag로 push합니다.
- `ai_openai_model`은 root variable에 기본값이 없으므로 `terraform.tfvars`에 반드시 지정합니다.
