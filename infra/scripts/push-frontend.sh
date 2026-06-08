#!/bin/bash
# Build the React frontend and upload the Vite dist output to the private S3 bucket.

set -e

PROFILE="${1:-my-lab}"
REGION="us-east-1"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
TERRAFORM_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
FRONTEND_DIR="$(cd "$SCRIPT_DIR/../../apps/frontend" && pwd)"
DIST_DIR="$FRONTEND_DIR/dist"

echo "=== DocGraph frontend build & S3 sync ==="
echo "Profile : $PROFILE"
echo "Region  : $REGION"
echo ""

if ! aws sts get-caller-identity --profile "$PROFILE" --region "$REGION" > /dev/null 2>&1; then
  echo "AWS credentials are invalid or expired. Run ./scripts/update_credentials.sh $PROFILE first."
  exit 1
fi

BUCKET=$(terraform -chdir="$TERRAFORM_DIR" output -raw frontend_bucket_name 2>/dev/null)
if [ -z "$BUCKET" ]; then
  echo "frontend_bucket_name is not available. Run terraform apply first."
  exit 1
fi

echo ">>> Build frontend"
npm --prefix "$FRONTEND_DIR" run build

echo ""
echo ">>> Sync immutable assets"
aws s3 sync "$DIST_DIR" "s3://$BUCKET" \
  --delete \
  --exclude "index.html" \
  --cache-control "public,max-age=31536000,immutable" \
  --profile "$PROFILE" \
  --region "$REGION"

echo ""
echo ">>> Upload index.html without long-lived cache"
aws s3 cp "$DIST_DIR/index.html" "s3://$BUCKET/index.html" \
  --cache-control "no-cache" \
  --content-type "text/html" \
  --profile "$PROFILE" \
  --region "$REGION"

echo ""
echo "Frontend uploaded to s3://$BUCKET"
