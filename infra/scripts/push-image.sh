#!/bin/bash
# ECR 이미지 빌드 & Push 스크립트 (Learner Lab 용)
#
# 사용법:
#   ./scripts/push-image.sh           # 기본 profile(my-lab), tag(latest)
#   ./scripts/push-image.sh my-lab    # profile 지정
#
# 사전 조건:
#   1. ./scripts/update_credentials.sh 로 자격증명 갱신
#   2. terraform apply 로 ECR 리포지터리 생성 완료

set -e

PROFILE="${1:-my-lab}"
REGION="us-east-1"
TAG="latest"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
TERRAFORM_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
BACKEND_DIR="$(cd "$SCRIPT_DIR/../../apps/backend" && pwd)"

echo "=== DocGraph 이미지 빌드 & ECR Push ==="
echo "Profile : $PROFILE"
echo "Region  : $REGION"
echo "Tag     : $TAG"
echo ""

# 자격증명 확인
if ! aws sts get-caller-identity --profile "$PROFILE" --region "$REGION" > /dev/null 2>&1; then
  echo "❌ 자격증명 만료. ./scripts/update_credentials.sh $PROFILE 를 먼저 실행하세요."
  exit 1
fi

# ECR URL은 terraform output에서 가져옴 (terraform apply 완료 후에만 사용 가능)
REPO_URL=$(terraform -chdir="$TERRAFORM_DIR" output -raw ecr_repository_url 2>/dev/null)
if [ -z "$REPO_URL" ]; then
  echo "❌ ecr_repository_url 을 가져올 수 없습니다. terraform apply 가 완료됐는지 확인하세요."
  exit 1
fi

REGISTRY="${REPO_URL%%/*}"
echo "ECR Repository: $REPO_URL"
echo ""

# ECR 로그인
echo ">>> ECR 로그인"
aws ecr get-login-password --region "$REGION" --profile "$PROFILE" \
  | docker login --username AWS --password-stdin "$REGISTRY"

# Docker 이미지 빌드 (멀티스테이지 빌드 — 첫 실행은 5~10분 소요)
echo ""
echo ">>> Docker 이미지 빌드 (apps/backend)"
docker build -t "${REPO_URL}:${TAG}" "$BACKEND_DIR"

# ECR Push
echo ""
echo ">>> ECR Push"
docker push "${REPO_URL}:${TAG}"

echo ""
echo "✅ Push 완료: ${REPO_URL}:${TAG}"
echo ""
echo "=== ECS 서비스 재배포 (선택) ==="
echo "이미지를 업데이트한 경우 아래 명령으로 ECS 서비스를 강제 재배포하세요:"
echo ""
echo "  aws ecs update-service \\"
echo "    --cluster app-cluster \\"
echo "    --service app-service \\"
echo "    --force-new-deployment \\"
echo "    --profile $PROFILE \\"
echo "    --region $REGION"
echo ""
echo "배포 상태 확인:"
echo "  aws ecs describe-services --cluster app-cluster --services app-service --profile $PROFILE --region $REGION \\"
echo "    --query 'services[0].{status:status,running:runningCount,desired:desiredCount,deployments:deployments[*].{status:status,count:runningCount}}'"
