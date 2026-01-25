#!/bin/bash

# Helper script to show what secrets to add to GitHub
# Usage: ./show-secrets-for-github.sh

set -e

if [ ! -f .env ]; then
  echo "❌ .env file not found!"
  echo "Run: cp .env.example .env"
  exit 1
fi

source .env

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  GitHub Secrets to Add"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "Go to: GitHub repo → Settings → Secrets and variables → Actions"
echo ""
echo "Add these secrets:"
echo ""

echo "1. AWS_ACCOUNT_ID"
echo "   Value: $(aws sts get-caller-identity --query Account --output text 2>/dev/null || echo 'Run: aws sts get-caller-identity')"
echo ""

echo "2. AWS_ROLE_ARN"
echo "   Value: (Check GitHub: Settings → Secrets → AWS_ROLE_ARN)"
echo "   Or run: aws iam list-roles --query 'Roles[?contains(RoleName, \`github\`)].Arn' --output text"
echo ""

echo "3. ROUTE53_HOSTED_ZONE_ID"
echo "   Value: Z08382803VF3OQN1IMW8S"
echo ""

echo "4. DB_SECURITY_GROUP_ID"
echo "   Value: sg-0f427a8f1823d7e4e"
echo ""

echo "5. SPRING_DATASOURCE_URL"
echo "   Value: ${SPRING_DATASOURCE_URL}"
echo ""

echo "6. DB_USERNAME"
echo "   Value: ${DB_USERNAME}"
echo ""

echo "7. DB_PASSWORD"
echo "   Value: ${DB_PASSWORD}"
echo ""

echo "8. JWT_SECRET"
echo "   Value: ${JWT_SECRET}"
echo ""

echo "9. B2_ENDPOINT"
echo "   Value: ${B2_ENDPOINT}"
echo ""

echo "10. B2_ACCESS_KEY_ID"
echo "    Value: ${B2_ACCESS_KEY_ID}"
echo ""

echo "11. B2_SECRET_ACCESS_KEY"
echo "    Value: ${B2_SECRET_ACCESS_KEY}"
echo ""

echo "12. B2_BUCKET_NAME"
echo "    Value: ${B2_BUCKET_NAME}"
echo ""

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "✅ After adding secrets, test by:"
echo "   1. Edit cdk.json (add a comment)"
echo "   2. git push"
echo "   3. Check Actions tab"
echo ""
