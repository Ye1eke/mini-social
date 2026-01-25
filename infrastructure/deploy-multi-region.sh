#!/bin/bash

echo "🌍 Multi-Region Deployment Script"
echo "=================================="
echo ""

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Check if AWS CLI is configured
if ! aws sts get-caller-identity &> /dev/null; then
    echo -e "${RED}❌ AWS CLI not configured${NC}"
    echo "Run: aws configure"
    exit 1
fi

ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
echo -e "${GREEN}✓ AWS Account: $ACCOUNT_ID${NC}"
echo ""

# Regions to deploy
REGIONS=("eu-central-1" "us-east-1")

echo "📍 Regions to deploy:"
for region in "${REGIONS[@]}"; do
    echo "  - $region"
done
echo ""

# Step 1: Bootstrap regions
echo "Step 1: Bootstrapping regions..."
echo "================================"
for region in "${REGIONS[@]}"; do
    echo -n "Bootstrapping $region... "
    if aws cloudformation describe-stacks --stack-name CDKToolkit --region $region &> /dev/null; then
        echo -e "${GREEN}✓ Already bootstrapped${NC}"
    else
        echo -e "${YELLOW}Bootstrapping...${NC}"
        cdk bootstrap aws://$ACCOUNT_ID/$region
        if [ $? -eq 0 ]; then
            echo -e "${GREEN}✓ Bootstrap complete${NC}"
        else
            echo -e "${RED}✗ Bootstrap failed${NC}"
            exit 1
        fi
    fi
done
echo ""

# Step 2: Deploy infrastructure
echo "Step 2: Deploying infrastructure..."
echo "===================================="
echo -e "${YELLOW}This will take 20-30 minutes...${NC}"
echo ""

read -p "Deploy to all regions? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    cdk deploy --all --require-approval never
    
    if [ $? -eq 0 ]; then
        echo ""
        echo -e "${GREEN}✅ Deployment successful!${NC}"
        echo ""
        
        # Get environment URLs
        echo "🌐 Environment URLs:"
        echo "==================="
        for region in "${REGIONS[@]}"; do
            region_short=${region//-/}
            env_name="minisocial-backend-cdk-$region_short"
            echo -n "$region: "
            aws elasticbeanstalk describe-environments \
                --environment-names $env_name \
                --region $region \
                --query 'Environments[0].CNAME' \
                --output text 2>/dev/null || echo "Not found"
        done
        echo ""
        
        # Check health
        echo "🏥 Health Status:"
        echo "================"
        for region in "${REGIONS[@]}"; do
            region_short=${region//-/}
            env_name="minisocial-backend-cdk-$region_short"
            echo -n "$region: "
            aws elasticbeanstalk describe-environment-health \
                --environment-name $env_name \
                --attribute-names HealthStatus \
                --region $region \
                --query 'HealthStatus' \
                --output text 2>/dev/null || echo "Unknown"
        done
        echo ""
        
        echo -e "${GREEN}🎉 Multi-region deployment complete!${NC}"
        echo ""
        echo "Next steps:"
        echo "  1. Deploy your application to each region"
        echo "  2. Test endpoints from different locations"
        echo "  3. Set up Route 53 for global routing (optional)"
        echo ""
        echo "See MULTI_REGION_GUIDE.md for details"
    else
        echo -e "${RED}✗ Deployment failed${NC}"
        exit 1
    fi
else
    echo "Deployment cancelled"
    exit 0
fi
