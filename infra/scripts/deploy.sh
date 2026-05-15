#!/bin/bash
# DocGraph 전체 배포 스크립트 (Learner Lab 용)
#
# 사용법: ./scripts/deploy.sh [profile]
#
# 최초 배포 / 코드 수정 재배포 양쪽에서 사용 가능.
#
# 순서:
#   1. 자격증명 확인
#   2. ECR 리포지터리만 먼저 생성 (이미 있으면 no-op)
#   3. Docker 이미지 빌드 & ECR Push
#   4. 나머지 인프라 전체 apply (VPC → RDS → ALB → ECS)
#   5. ECS 서비스 강제 재배포 (새 이미지 반영)
#   6. 접속 정보 출력

set -e

PROFILE="${1:-my-lab}"
REGION="us-east-1"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
TERRAFORM_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "========================================="
echo " DocGraph 배포 (Learner Lab)"
echo " Profile: $PROFILE | Region: $REGION"
echo "========================================="
echo ""

# ── Step 0: 자격증명 확인 ─────────────────────────────────────────────────────
echo ">>> [0/5] 자격증명 확인"
if ! aws sts get-caller-identity --profile "$PROFILE" --region "$REGION" > /dev/null 2>&1; then
  echo "❌ 자격증명 만료"
  echo "   ./scripts/update_credentials.sh $PROFILE 를 실행한 뒤 다시 시도하세요."
  exit 1
fi
echo "✅ 자격증명 유효"
echo ""

# ── Step 1: ECR 리포지터리 먼저 생성 ─────────────────────────────────────────
# 이미지를 Push할 대상이 존재해야 하므로 ECR을 인프라 전체보다 먼저 apply한다.
# 이미 존재하는 경우 terraform이 "No changes"로 즉시 통과한다.
echo ">>> [1/5] ECR 리포지터리 생성 (이미 있으면 no-op)"
terraform -chdir="$TERRAFORM_DIR" apply -target=module.ecr -auto-approve
echo ""

# ── Step 2: Docker 이미지 빌드 & ECR Push ────────────────────────────────────
# ECR이 준비된 상태에서 이미지를 Push한다.
# 첫 빌드는 Gradle 의존성 다운로드 + Kotlin 컴파일로 5~10분 소요된다.
echo ">>> [2/5] Docker 이미지 빌드 & ECR Push"
"$SCRIPT_DIR/push-image.sh" "$PROFILE"
echo ""

# ── Step 3: 나머지 인프라 전체 apply ─────────────────────────────────────────
# ECR에 이미지가 있는 상태에서 VPC → RDS → ALB → ECS 순서로 생성/업데이트한다.
# RDS 최초 생성 시 10~15분 소요된다.
echo ">>> [3/5] 인프라 전체 apply (RDS 포함 시 10~15분 소요)"
terraform -chdir="$TERRAFORM_DIR" apply -auto-approve
echo ""

# ── Step 4: ECS 서비스 강제 재배포 ───────────────────────────────────────────
# terraform이 task definition을 변경하지 않은 경우(이미지 태그만 바뀐 경우 등)에도
# ECS가 새 이미지를 pull하도록 강제 재배포를 트리거한다.
echo ">>> [4/5] ECS 서비스 강제 재배포"
aws ecs update-service \
  --cluster app-cluster \
  --service app-service \
  --force-new-deployment \
  --profile "$PROFILE" \
  --region "$REGION" \
  --output text \
  --query 'service.serviceArn' > /dev/null
echo "✅ 재배포 요청 완료"
echo ""

# ── Step 5: 접속 정보 출력 ────────────────────────────────────────────────────
SWAGGER_URL=$(terraform -chdir="$TERRAFORM_DIR" output -raw swagger_ui_url 2>/dev/null)
ALB_DNS=$(terraform -chdir="$TERRAFORM_DIR" output -raw alb_dns_name 2>/dev/null)

echo "========================================="
echo " 배포 완료"
echo "========================================="
echo " Swagger UI : $SWAGGER_URL"
echo " ALB DNS    : $ALB_DNS"
echo ""
echo " ECS 태스크 안정화까지 약 3~5분 소요됩니다."
echo " (health_check_grace_period 180초 + ALB healthy_threshold 2회)"
echo ""
echo " 상태 확인:"
echo "   aws ecs describe-services \\"
echo "     --cluster app-cluster --services app-service \\"
echo "     --profile $PROFILE --region $REGION \\"
echo "     --query 'services[0].deployments[*].{status:status,running:runningCount,desired:desiredCount}'"
echo ""
echo " 기동 로그:"
echo "   aws logs tail /ecs/app --follow --profile $PROFILE --region $REGION"
echo "========================================="
