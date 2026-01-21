#!/bin/bash

echo "🔍 Checking prerequisites for CDK deployment..."
echo ""

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check Node.js
echo -n "Checking Node.js... "
if command -v node &> /dev/null; then
    NODE_VERSION=$(node --version)
    echo -e "${GREEN}✓ Found $NODE_VERSION${NC}"
else
    echo -e "${RED}✗ Not found${NC}"
    echo "  Install from: https://nodejs.org/"
    exit 1
fi

# Check npm
echo -n "Checking npm... "
if command -v npm &> /dev/null; then
    NPM_VERSION=$(npm --version)
    echo -e "${GREEN}✓ Found v$NPM_VERSION${NC}"
else
    echo -e "${RED}✗ Not found${NC}"
    exit 1
fi

# Check AWS CLI
echo -n "Checking AWS CLI... "
if command -v aws &> /dev/null; then
    AWS_VERSION=$(aws --version 2>&1 | cut -d' ' -f1)
    echo -e "${GREEN}✓ Found $AWS_VERSION${NC}"
else
    echo -e "${RED}✗ Not found${NC}"
    echo "  Install: brew install awscli"
    echo "  Or visit: https://aws.amazon.com/cli/"
    exit 1
fi

# Check AWS credentials
echo -n "Checking AWS credentials... "
if aws sts get-caller-identity &> /dev/null; then
    AWS_ACCOUNT=$(aws sts get-caller-identity --query Account --output text)
    AWS_REGION=$(aws configure get region)
    echo -e "${GREEN}✓ Configured${NC}"
    echo "  Account: $AWS_ACCOUNT"
    echo "  Region: $AWS_REGION"
else
    echo -e "${RED}✗ Not configured${NC}"
    echo "  Run: aws configure"
    exit 1
fi

# Check CDK CLI
echo -n "Checking AWS CDK CLI... "
if command -v cdk &> /dev/null; then
    CDK_VERSION=$(cdk --version)
    echo -e "${GREEN}✓ Found $CDK_VERSION${NC}"
else
    echo -e "${YELLOW}✗ Not found${NC}"
    echo "  Install: npm install -g aws-cdk"
    echo "  This is required to deploy!"
    exit 1
fi

# Check if dependencies are installed
echo -n "Checking project dependencies... "
if [ -d "node_modules" ]; then
    echo -e "${GREEN}✓ Installed${NC}"
else
    echo -e "${YELLOW}✗ Not installed${NC}"
    echo "  Run: npm install"
fi

# Check default VPC
echo -n "Checking default VPC... "
DEFAULT_VPC=$(aws ec2 describe-vpcs --filters "Name=isDefault,Values=true" --query 'Vpcs[0].VpcId' --output text 2>/dev/null)
if [ "$DEFAULT_VPC" != "None" ] && [ -n "$DEFAULT_VPC" ]; then
    echo -e "${GREEN}✓ Found $DEFAULT_VPC${NC}"
else
    echo -e "${YELLOW}⚠ Not found${NC}"
    echo "  You may need to create one: aws ec2 create-default-vpc"
fi

# Check if CDK is bootstrapped
echo -n "Checking CDK bootstrap... "
BOOTSTRAP_STACK=$(aws cloudformation describe-stacks --stack-name CDKToolkit --query 'Stacks[0].StackStatus' --output text 2>/dev/null)
if [ "$BOOTSTRAP_STACK" == "CREATE_COMPLETE" ] || [ "$BOOTSTRAP_STACK" == "UPDATE_COMPLETE" ]; then
    echo -e "${GREEN}✓ Bootstrapped${NC}"
else
    echo -e "${YELLOW}⚠ Not bootstrapped${NC}"
    echo "  Run: cdk bootstrap aws://$AWS_ACCOUNT/$AWS_REGION"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo -e "${GREEN}✓ All prerequisites met!${NC}"
echo ""
echo "Next steps:"
echo "  1. npm install (if not done)"
echo "  2. Edit cdk.json with your configuration"
echo "  3. npm run synth (to preview)"
echo "  4. npm run deploy (to deploy)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
