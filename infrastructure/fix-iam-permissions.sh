#!/bin/bash

# Fix IAM role permissions for GitOps workflow
# Adds necessary permissions for CDK deployment

set -e

ROLE_NAME="github-actions"

echo "🔧 Adding IAM permissions for GitOps workflow"
echo ""
echo "Role: $ROLE_NAME"
echo ""

# Create policy document with all required permissions
cat > /tmp/gitops-policy.json <<'EOF'
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "CDKBootstrapCheck",
      "Effect": "Allow",
      "Action": [
        "ssm:GetParameter"
      ],
      "Resource": [
        "arn:aws:ssm:*:*:parameter/cdk-bootstrap/*"
      ]
    },
    {
      "Sid": "CDKDeployment",
      "Effect": "Allow",
      "Action": [
        "cloudformation:*",
        "s3:*",
        "iam:PassRole",
        "iam:GetRole",
        "iam:CreateRole",
        "iam:AttachRolePolicy",
        "iam:PutRolePolicy"
      ],
      "Resource": "*"
    },
    {
      "Sid": "ElasticBeanstalk",
      "Effect": "Allow",
      "Action": [
        "elasticbeanstalk:*",
        "ec2:*",
        "elasticloadbalancing:*",
        "autoscaling:*",
        "cloudwatch:*",
        "logs:*"
      ],
      "Resource": "*"
    },
    {
      "Sid": "Route53",
      "Effect": "Allow",
      "Action": [
        "route53:*"
      ],
      "Resource": "*"
    },
    {
      "Sid": "RDS",
      "Effect": "Allow",
      "Action": [
        "rds:Describe*",
        "ec2:AuthorizeSecurityGroupIngress",
        "ec2:DescribeSecurityGroups",
        "ec2:DescribeInstances"
      ],
      "Resource": "*"
    }
  ]
}
EOF

echo "📝 Policy to attach:"
cat /tmp/gitops-policy.json
echo ""

# Check if policy already exists
POLICY_NAME="GitHubActionsGitOpsPolicy"
POLICY_ARN="arn:aws:iam::$(aws sts get-caller-identity --query Account --output text):policy/$POLICY_NAME"

if aws iam get-policy --policy-arn "$POLICY_ARN" >/dev/null 2>&1; then
  echo "⚠️  Policy already exists, creating new version..."
  
  # Delete old versions if at limit
  VERSIONS=$(aws iam list-policy-versions --policy-arn "$POLICY_ARN" --query 'Versions[?!IsDefaultVersion].[VersionId]' --output text)
  for VERSION in $VERSIONS; do
    echo "   Deleting old version: $VERSION"
    aws iam delete-policy-version --policy-arn "$POLICY_ARN" --version-id "$VERSION" 2>/dev/null || true
  done
  
  # Create new version
  aws iam create-policy-version \
    --policy-arn "$POLICY_ARN" \
    --policy-document file:///tmp/gitops-policy.json \
    --set-as-default
  
  echo "   ✅ Policy updated"
else
  echo "📋 Creating new policy..."
  aws iam create-policy \
    --policy-name "$POLICY_NAME" \
    --policy-document file:///tmp/gitops-policy.json \
    --description "Permissions for GitHub Actions GitOps workflow"
  
  echo "   ✅ Policy created"
fi

# Attach policy to role
echo ""
echo "🔗 Attaching policy to role..."
aws iam attach-role-policy \
  --role-name "$ROLE_NAME" \
  --policy-arn "$POLICY_ARN" 2>/dev/null || echo "   ℹ️  Policy already attached"

echo ""
echo "✅ IAM permissions configured!"
echo ""
echo "The role now has permissions for:"
echo "  ✅ SSM Parameter Store (CDK bootstrap check)"
echo "  ✅ CloudFormation (infrastructure deployment)"
echo "  ✅ Elastic Beanstalk (application environments)"
echo "  ✅ Route 53 (DNS management)"
echo "  ✅ EC2 & Security Groups (database access)"
echo ""
echo "🚀 GitOps workflow is ready!"
echo "   Test with: git push"
echo ""
