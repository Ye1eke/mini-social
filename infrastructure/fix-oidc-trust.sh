#!/bin/bash

# Fix OIDC trust policy to allow infrastructure workflow
# This updates the IAM role to trust GitHub Actions from your repo

set -e

ROLE_NAME="github-actions"
REPO="Ye1eke/mini-social"

echo "🔧 Updating IAM role trust policy for GitOps"
echo ""
echo "Role: $ROLE_NAME"
echo "Repo: $REPO"
echo ""

# Create updated trust policy
cat > /tmp/trust-policy.json <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "arn:aws:iam::805425386074:oidc-provider/token.actions.githubusercontent.com"
      },
      "Action": "sts:AssumeRoleWithWebIdentity",
      "Condition": {
        "StringEquals": {
          "token.actions.githubusercontent.com:aud": "sts.amazonaws.com"
        },
        "StringLike": {
          "token.actions.githubusercontent.com:sub": [
            "repo:$REPO:ref:refs/heads/main",
            "repo:$REPO:environment:AWS"
          ]
        }
      }
    }
  ]
}
EOF

echo "📝 New trust policy:"
cat /tmp/trust-policy.json
echo ""

echo "🔄 Updating IAM role..."
aws iam update-assume-role-policy \
  --role-name "$ROLE_NAME" \
  --policy-document file:///tmp/trust-policy.json

echo ""
echo "✅ Trust policy updated!"
echo ""
echo "The role now trusts:"
echo "  ✅ repo:$REPO:ref:refs/heads/main (any workflow on main branch)"
echo "  ✅ repo:$REPO:environment:AWS (workflows using AWS environment)"
echo ""
echo "You can now enable the GitOps workflow!"
