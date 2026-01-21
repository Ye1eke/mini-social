#!/bin/bash

echo "🔍 Gathering AWS values for cdk.json configuration..."
echo ""

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Check AWS CLI
if ! command -v aws &> /dev/null; then
    echo "❌ AWS CLI not found. Please install it first."
    exit 1
fi

# Check credentials
if ! aws sts get-caller-identity &> /dev/null; then
    echo "❌ AWS credentials not configured. Run: aws configure"
    exit 1
fi

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}AWS Account Information${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
REGION=$(aws configure get region)

echo "Account ID: $ACCOUNT_ID"
echo "Region: $REGION"
echo ""

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}Elastic Beanstalk Applications${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

EB_APPS=$(aws elasticbeanstalk describe-applications --query 'Applications[*].ApplicationName' --output text 2>/dev/null)
if [ -n "$EB_APPS" ]; then
    echo "Found applications:"
    echo "$EB_APPS" | tr '\t' '\n' | sed 's/^/  - /'
    echo ""
    echo -e "${YELLOW}Recommended for cdk.json:${NC}"
    FIRST_APP=$(echo "$EB_APPS" | awk '{print $1}')
    echo "  \"applicationName\": \"$FIRST_APP\""
else
    echo "No existing applications found."
    echo -e "${YELLOW}Recommended for cdk.json:${NC}"
    echo "  \"applicationName\": \"minisocial\""
fi
echo ""

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}Elastic Beanstalk Environments${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

EB_ENVS=$(aws elasticbeanstalk describe-environments --query 'Environments[*].[EnvironmentName,CNAME,Status]' --output text 2>/dev/null)
if [ -n "$EB_ENVS" ]; then
    echo "Existing environments:"
    echo "$EB_ENVS" | while read name cname status; do
        echo "  - $name ($status)"
        echo "    URL: $cname"
    done
    echo ""
    echo -e "${YELLOW}⚠️  Use a NEW name to avoid conflicts:${NC}"
    echo "  \"environmentName\": \"minisocial-backend-cdk\""
else
    echo "No existing environments found."
    echo -e "${YELLOW}Recommended for cdk.json:${NC}"
    echo "  \"environmentName\": \"minisocial-backend-cdk\""
fi
echo ""

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}RDS Databases${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

RDS_DBS=$(aws rds describe-db-instances --query 'DBInstances[*].[DBInstanceIdentifier,Endpoint.Address,Endpoint.Port,Engine]' --output text 2>/dev/null)
if [ -n "$RDS_DBS" ]; then
    echo "Found databases:"
    echo "$RDS_DBS" | while read id endpoint port engine; do
        echo "  - $id ($engine)"
        echo "    Endpoint: $endpoint:$port"
        if [ "$engine" == "postgres" ]; then
            echo ""
            echo -e "${YELLOW}For cdk.json:${NC}"
            echo "  \"SPRING_DATASOURCE_URL\": \"jdbc:postgresql://$endpoint:$port/minisocial\""
        fi
    done
else
    echo "No RDS databases found."
    echo -e "${YELLOW}You can:${NC}"
    echo "  1. Create RDS manually in AWS Console"
    echo "  2. Let Elastic Beanstalk create it (see CONFIGURATION_GUIDE.md)"
    echo "  3. Use external database"
fi
echo ""

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}ElastiCache Redis${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

REDIS_CLUSTERS=$(aws elasticache describe-cache-clusters --show-cache-node-info --query 'CacheClusters[*].[CacheClusterId,CacheNodes[0].Endpoint.Address,CacheNodes[0].Endpoint.Port]' --output text 2>/dev/null)
if [ -n "$REDIS_CLUSTERS" ]; then
    echo "Found Redis clusters:"
    echo "$REDIS_CLUSTERS" | while read id endpoint port; do
        echo "  - $id"
        echo "    Endpoint: $endpoint:$port"
        echo ""
        echo -e "${YELLOW}For cdk.json:${NC}"
        echo "  \"REDIS_HOST\": \"$endpoint\""
        echo "  \"REDIS_PORT\": \"$port\""
    done
else
    echo "No ElastiCache clusters found."
    echo -e "${YELLOW}For now, use placeholder:${NC}"
    echo "  \"REDIS_HOST\": \"localhost\""
    echo "  \"REDIS_PORT\": \"6379\""
fi
echo ""

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}ACM Certificates (for HTTPS)${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

CERTS=$(aws acm list-certificates --query 'CertificateSummaryList[*].[DomainName,CertificateArn,Status]' --output text 2>/dev/null)
if [ -n "$CERTS" ]; then
    echo "Found certificates:"
    echo "$CERTS" | while read domain arn status; do
        echo "  - $domain ($status)"
        echo "    ARN: $arn"
        if [ "$status" == "ISSUED" ]; then
            echo ""
            echo -e "${YELLOW}For cdk.json:${NC}"
            echo "  \"certificateArn\": \"$arn\""
        fi
    done
else
    echo "No ACM certificates found."
    echo -e "${YELLOW}For HTTP only (no HTTPS):${NC}"
    echo "  \"certificateArn\": \"\""
fi
echo ""

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}Platform Version (Solution Stack)${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

LATEST_STACK=$(aws elasticbeanstalk list-available-solution-stacks --query 'SolutionStacks[?contains(@, `Corretto 17`) && contains(@, `Amazon Linux 2023`)] | [0]' --output text 2>/dev/null)
if [ -n "$LATEST_STACK" ] && [ "$LATEST_STACK" != "None" ]; then
    echo "Latest Corretto 17 platform:"
    echo "  $LATEST_STACK"
    echo ""
    echo -e "${YELLOW}For cdk.json:${NC}"
    echo "  \"solutionStackName\": \"$LATEST_STACK\""
else
    echo "Could not fetch latest platform."
    echo -e "${YELLOW}Use this default:${NC}"
    echo "  \"solutionStackName\": \"64bit Amazon Linux 2023 v4.3.0 running Corretto 17\""
fi
echo ""

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}Summary${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo "Next steps:"
echo "  1. Edit infrastructure/cdk.json with the values above"
echo "  2. Add your database credentials (DB_USERNAME, DB_PASSWORD)"
echo "  3. Add your B2 credentials (if you have them)"
echo "  4. Generate a JWT secret: openssl rand -base64 32"
echo "  5. Run: npm run synth (to preview)"
echo "  6. Run: npm run deploy (to deploy)"
echo ""
echo "See CONFIGURATION_GUIDE.md for detailed instructions."
