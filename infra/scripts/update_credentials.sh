#!/bin/bash
# 사용법: ./scripts/update_credentials.sh <profile-name>
# 예시:   ./scripts/update_credentials.sh member-a

PROFILE=${1:-my-lab}

echo "=== Learner Lab 자격증명 갱신 ==="
echo "Profile: $PROFILE"
echo ""
echo "Learner Lab 콘솔 → AWS Details → AWS CLI 클릭 후 아래에 붙여넣으세요."
echo ""

read -p "aws_access_key_id     : " KEY_ID
read -p "aws_secret_access_key : " SECRET
read -p "aws_session_token     : " TOKEN

aws configure set aws_access_key_id "$KEY_ID" --profile "$PROFILE"
aws configure set aws_secret_access_key "$SECRET" --profile "$PROFILE"
aws configure set aws_session_token "$TOKEN" --profile "$PROFILE"
aws configure set region "us-east-1" --profile "$PROFILE"

echo ""
echo "✅ [$PROFILE] 자격증명 갱신 완료"
echo "확인: aws sts get-caller-identity --profile $PROFILE"
