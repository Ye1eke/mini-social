#!/bin/bash

# Comprehensive local test of GitOps workflow logic
# This simulates what GitHub Actions will do
# Usage: ./test-gitops-locally.sh

set -e

cd "$(dirname "$0")"

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Testing GitOps Workflow Locally"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Test 1: Check AWS credentials
echo "✅ Test 1: AWS Credentials"
if aws sts get-caller-identity >/dev/null 2>&1; then
  ACCOUNT=$(aws sts get-caller-identity --query Account --output text)
  echo "   Account: $ACCOUNT"
else
  echo "   ❌ AWS credentials not configured"
  exit 1
fi
echo ""

# Test 2: Read regions from cdk.json
echo "✅ Test 2: Read Regions from cdk.json"
REGIONS=$(jq -c '.context.regions' cdk.json)
if [ "$REGIONS" == "null" ] || [ -z "$REGIONS" ]; then
  echo "   ❌ Failed to read regions from cdk.json"
  exit 1
fi
echo "   Regions: $REGIONS"
echo ""

# Test 3: Check IAM permissions
echo "✅ Test 3: Check IAM Permissions"
echo "   Testing SSM access..."
if aws ssm get-parameter --name /cdk-bootstrap/hnb659fds/version --region eu-central-1 >/dev/null 2>&1; then
  echo "   ✅ SSM access: OK"
else
  echo "   ⚠️  SSM access: Limited (might fail in GitHub Actions)"
fi

echo "   Testing CloudFormation access..."
if aws cloudformation list-stacks --region eu-central-1 --max-results 1 >/dev/null 2>&1; then
  echo "   ✅ CloudFormation access: OK"
else
  echo "   ❌ CloudFormation access: FAILED"
  exit 1
fi

echo "   Testing Elastic Beanstalk access..."
if aws elasticbeanstalk describe-applications --region eu-central-1 >/dev/null 2>&1; then
  echo "   ✅ Elastic Beanstalk access: OK"
else
  echo "   ❌ Elastic Beanstalk access: FAILED"
  exit 1
fi
echo ""

# Test 4: Check if regions are bootstrapped
echo "✅ Test 4: Check CDK Bootstrap Status"
for REGION in $(echo "$REGIONS" | jq -r '.[]'); do
  echo "   Checking $REGION..."
  if aws ssm get-parameter --name /cdk-bootstrap/hnb659fds/version --region "$REGION" >/dev/null 2>&1; then
    VERSION=$(aws ssm get-parameter --name /cdk-bootstrap/hnb659fds/version --region "$REGION" --query 'Parameter.Value' --output text)
    echo "   ✅ $REGION: Bootstrapped (version $VERSION)"
  else
    echo "   ⚠️  $REGION: Not bootstrapped (will auto-bootstrap)"
  fi
done
echo ""

# Test 5: Check environment variables
echo "✅ Test 5: Check Environment Variables"
if [ -f .env ]; then
  source .env
  REQUIRED_VARS=(
    "SPRING_DATASOURCE_URL"
    "DB_USERNAME"
    "DB_PASSWORD"
    "JWT_SECRET"
    "SERVER_PORT"
  )
  
  ALL_PRESENT=true
  for VAR in "${REQUIRED_VARS[@]}"; do
    if [ -z "${!VAR}" ]; then
      echo "   ❌ Missing: $VAR"
      ALL_PRESENT=false
    else
      echo "   ✅ Present: $VAR"
    fi
  done
  
  if [ "$ALL_PRESENT" = false ]; then
    echo ""
    echo "   ⚠️  Some environment variables are missing"
    echo "   These need to be added as GitHub Secrets"
  fi
else
  echo "   ⚠️  .env file not found"
fi
echo ""

# Test 6: Simulate CDK synth
echo "✅ Test 6: CDK Synthesis"
echo "   Running: cdk synth (dry run)..."
if cdk synth >/dev/null 2>&1; then
  echo "   ✅ CDK synthesis: OK"
else
  echo "   ❌ CDK synthesis: FAILED"
  echo "   Run 'npm install' if dependencies are missing"
  exit 1
fi
echo ""

# Test 7: Check existing infrastructure
echo "✅ Test 7: Check Existing Infrastructure"
for REGION in $(echo "$REGIONS" | jq -r '.[]'); do
  REGION_SHORT=$(echo "$REGION" | tr -d '-')
  ENV_NAME="minisocial-backend-cdk-${REGION_SHORT}"
  
  echo "   Checking $ENV_NAME in $REGION..."
  if aws elasticbeanstalk describe-environments \
    --environment-names "$ENV_NAME" \
    --region "$REGION" \
    --query 'Environments[0].Status' \
    --output text 2>/dev/null | grep -q "Ready"; then
    echo "   ✅ Environment exists and ready"
  else
    echo "   ⚠️  Environment not found (will be created)"
  fi
done
echo ""

# Test 8: Check Route 53
echo "✅ Test 8: Check Route 53 Configuration"
HOSTED_ZONE_ID="Z08382803VF3OQN1IMW8S"
DOMAIN="api.minisocial.online"

if aws route53 list-resource-record-sets \
  --hosted-zone-id "$HOSTED_ZONE_ID" \
  --query "ResourceRecordSets[?Name=='$DOMAIN.']" \
  --output json | jq -e '. | length > 0' >/dev/null 2>&1; then
  echo "   ✅ Route 53 record exists"
else
  echo "   ⚠️  Route 53 record not found (will be created)"
fi
echo ""

# Summary
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Test Summary"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "✅ All critical tests passed!"
echo ""
echo "📋 What will happen in GitHub Actions:"
echo ""
for REGION in $(echo "$REGIONS" | jq -r '.[]'); do
  REGION_SHORT=$(echo "$REGION" | tr -d '-')
  echo "   1. Deploy to $REGION"
  echo "      - Stack: MiniSocialBackendEb-${REGION_SHORT}"
  echo "      - Environment: minisocial-backend-cdk-${REGION_SHORT}"
  echo ""
done
echo "   2. Update Route 53 with latency-based routing"
echo "   3. Configure database access automatically"
echo ""
echo "⚠️  Known Issues to Fix:"
echo "   - GitHub Actions role needs SSM:GetParameter permission"
echo "   - Run: ./fix-iam-permissions.sh"
echo ""
echo "🚀 Ready to deploy!"
echo "   Local: ./deploy.sh all"
echo "   GitOps: git push (after fixing IAM permissions)"
echo ""
