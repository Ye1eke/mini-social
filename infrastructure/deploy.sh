#!/bin/bash

# MiniSocial Infrastructure Deployment Script
# One script to rule them all!
# Usage: ./deploy.sh [command]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
HOSTED_ZONE_ID="Z08382803VF3OQN1IMW8S"
DOMAIN="api.minisocial.online"
DB_SECURITY_GROUP="sg-0f427a8f1823d7e4e"

print_header() {
  echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
  echo -e "${BLUE}  $1${NC}"
  echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
  echo ""
}

print_success() {
  echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
  echo -e "${RED}❌ $1${NC}"
}

print_warning() {
  echo -e "${YELLOW}⚠️  $1${NC}"
}

print_info() {
  echo -e "${BLUE}ℹ️  $1${NC}"
}

# Function: Deploy infrastructure to specific region
deploy_region() {
  local region=$1
  local region_short=$(echo "$region" | tr -d '-')
  local stack_name="MiniSocialBackendEb-${region_short}"
  local env_name="minisocial-backend-cdk-${region_short}"
  
  print_header "Deploying to $region"
  
  print_info "Stack: $stack_name"
  print_info "Environment: $env_name"
  echo ""
  
  # Check if region is bootstrapped
  print_info "Checking if region is bootstrapped..."
  if ! aws ssm get-parameter --name /cdk-bootstrap/hnb659fds/version --region "$region" >/dev/null 2>&1; then
    print_warning "Region not bootstrapped, bootstrapping now..."
    cdk bootstrap "aws://$(aws sts get-caller-identity --query Account --output text)/$region"
    print_success "Region bootstrapped!"
  else
    print_success "Region already bootstrapped"
  fi
  echo ""
  
  # Deploy with CDK
  cdk deploy "$stack_name" --require-approval never
  
  print_success "Infrastructure deployed to $region"
  echo ""
  
  # Wait for environment to be ready
  print_info "Waiting for environment to be ready..."
  sleep 10
  
  # Fix database access
  print_info "Configuring database access..."
  fix_db_access "$env_name" "$region"
  
  print_success "Region $region is ready!"
  echo ""
}

# Function: Fix database access for an environment
fix_db_access() {
  local env_name=$1
  local region=$2
  
  print_info "Getting instance from $env_name..."
  
  local instance_id=$(aws elasticbeanstalk describe-environment-resources \
    --environment-name "$env_name" \
    --region "$region" \
    --query 'EnvironmentResources.Instances[0].Id' \
    --output text 2>/dev/null || echo "")
  
  if [ -z "$instance_id" ] || [ "$instance_id" == "None" ]; then
    print_error "No instances found in $env_name"
    return 1
  fi
  
  print_success "Found instance: $instance_id"
  
  local eb_sg=$(aws ec2 describe-instances \
    --instance-ids "$instance_id" \
    --region "$region" \
    --query 'Reservations[0].Instances[0].SecurityGroups[0].GroupId' \
    --output text)
  
  print_success "Found security group: $eb_sg"
  
  print_info "Adding database access rule..."
  aws ec2 authorize-security-group-ingress \
    --group-id "$DB_SECURITY_GROUP" \
    --protocol tcp \
    --port 5432 \
    --source-group "$eb_sg" \
    --region "$region" 2>/dev/null && print_success "Database access granted" || print_warning "Rule might already exist (OK)"
  
  # Restart app server
  print_info "Restarting application..."
  aws elasticbeanstalk restart-app-server \
    --environment-name "$env_name" \
    --region "$region" >/dev/null 2>&1
  
  print_success "Application restarted"
}

# Function: Setup Route 53 latency-based routing
setup_route53() {
  print_header "Setting up Route 53 Latency-Based Routing"
  
  print_info "Domain: $DOMAIN"
  echo ""
  
  # Get EU backend
  print_info "Checking EU backend (eu-central-1)..."
  local eu_lb_arn=$(aws elasticbeanstalk describe-environment-resources \
    --environment-name minisocial-backend-cdk-eucentral1 \
    --region eu-central-1 \
    --query 'EnvironmentResources.LoadBalancers[0].Name' \
    --output text 2>/dev/null || echo "")
  
  if [ -z "$eu_lb_arn" ] || [ "$eu_lb_arn" == "None" ]; then
    print_error "EU backend not found. Deploy it first with: ./deploy.sh eu"
    exit 1
  fi
  
  local eu_lb_dns=$(aws elbv2 describe-load-balancers \
    --region eu-central-1 \
    --load-balancer-arns "$eu_lb_arn" \
    --query 'LoadBalancers[0].DNSName' \
    --output text)
  
  local eu_lb_zone=$(aws elbv2 describe-load-balancers \
    --region eu-central-1 \
    --load-balancer-arns "$eu_lb_arn" \
    --query 'LoadBalancers[0].CanonicalHostedZoneId' \
    --output text)
  
  print_success "EU: $eu_lb_dns"
  
  # Check US backend
  print_info "Checking US backend (us-east-1)..."
  local us_lb_arn=$(aws elasticbeanstalk describe-environment-resources \
    --environment-name minisocial-backend-cdk-useast1 \
    --region us-east-1 \
    --query 'EnvironmentResources.LoadBalancers[0].Name' \
    --output text 2>/dev/null || echo "")
  
  if [ -z "$us_lb_arn" ] || [ "$us_lb_arn" == "None" ]; then
    print_warning "US backend not deployed yet"
    print_info "Creating single-region routing (EU only)"
    
    # Single region
    cat > /tmp/route53-latency.json <<EOF
{
  "Comment": "Latency-based routing - EU only",
  "Changes": [
    {
      "Action": "UPSERT",
      "ResourceRecordSet": {
        "Name": "$DOMAIN",
        "Type": "A",
        "SetIdentifier": "EU-Frankfurt",
        "Region": "eu-central-1",
        "AliasTarget": {
          "HostedZoneId": "$eu_lb_zone",
          "DNSName": "dualstack.$eu_lb_dns",
          "EvaluateTargetHealth": true
        }
      }
    }
  ]
}
EOF
  else
    local us_lb_dns=$(aws elbv2 describe-load-balancers \
      --region us-east-1 \
      --load-balancer-arns "$us_lb_arn" \
      --query 'LoadBalancers[0].DNSName' \
      --output text)
    
    local us_lb_zone=$(aws elbv2 describe-load-balancers \
      --region us-east-1 \
      --load-balancer-arns "$us_lb_arn" \
      --query 'LoadBalancers[0].CanonicalHostedZoneId' \
      --output text)
    
    print_success "US: $us_lb_dns"
    print_info "Creating multi-region latency routing"
    
    # Multi-region
    cat > /tmp/route53-latency.json <<EOF
{
  "Comment": "Latency-based routing - multi-region",
  "Changes": [
    {
      "Action": "UPSERT",
      "ResourceRecordSet": {
        "Name": "$DOMAIN",
        "Type": "A",
        "SetIdentifier": "EU-Frankfurt",
        "Region": "eu-central-1",
        "AliasTarget": {
          "HostedZoneId": "$eu_lb_zone",
          "DNSName": "dualstack.$eu_lb_dns",
          "EvaluateTargetHealth": true
        }
      }
    },
    {
      "Action": "UPSERT",
      "ResourceRecordSet": {
        "Name": "$DOMAIN",
        "Type": "A",
        "SetIdentifier": "US-Virginia",
        "Region": "us-east-1",
        "AliasTarget": {
          "HostedZoneId": "$us_lb_zone",
          "DNSName": "dualstack.$us_lb_dns",
          "EvaluateTargetHealth": true
        }
      }
    }
  ]
}
EOF
  fi
  
  echo ""
  print_info "Updating Route 53..."
  
  # First, try to delete existing non-latency records if they exist
  print_info "Checking for existing non-latency records..."
  EXISTING_RECORDS=$(aws route53 list-resource-record-sets \
    --hosted-zone-id "$HOSTED_ZONE_ID" \
    --query "ResourceRecordSets[?Name=='$DOMAIN.' && Type=='A' && !SetIdentifier]" \
    --output json)
  
  if [ "$EXISTING_RECORDS" != "[]" ]; then
    print_warning "Found existing non-latency record, converting to latency-based..."
    
    # Get the existing record details
    EXISTING_DNS=$(echo "$EXISTING_RECORDS" | jq -r '.[0].AliasTarget.DNSName')
    EXISTING_ZONE=$(echo "$EXISTING_RECORDS" | jq -r '.[0].AliasTarget.HostedZoneId')
    
    # Delete the old record
    cat > /tmp/route53-delete.json <<EOF
{
  "Comment": "Delete non-latency record before creating latency-based",
  "Changes": [
    {
      "Action": "DELETE",
      "ResourceRecordSet": {
        "Name": "$DOMAIN",
        "Type": "A",
        "AliasTarget": {
          "HostedZoneId": "$EXISTING_ZONE",
          "DNSName": "$EXISTING_DNS",
          "EvaluateTargetHealth": true
        }
      }
    },
    {
      "Action": "DELETE",
      "ResourceRecordSet": {
        "Name": "$DOMAIN",
        "Type": "AAAA",
        "AliasTarget": {
          "HostedZoneId": "$EXISTING_ZONE",
          "DNSName": "$EXISTING_DNS",
          "EvaluateTargetHealth": true
        }
      }
    }
  ]
}
EOF
    
    aws route53 change-resource-record-sets \
      --hosted-zone-id "$HOSTED_ZONE_ID" \
      --change-batch file:///tmp/route53-delete.json 2>/dev/null || print_warning "Could not delete old records (might not exist)"
    
    print_info "Waiting for DNS propagation..."
    sleep 5
  fi
  
  aws route53 change-resource-record-sets \
    --hosted-zone-id "$HOSTED_ZONE_ID" \
    --change-batch file:///tmp/route53-latency.json
  
  echo ""
  print_success "Route 53 configured!"
  print_info "Users will automatically connect to nearest backend"
  print_info "DNS propagation: 1-5 minutes"
  echo ""
}

# Function: Show status
show_status() {
  print_header "Infrastructure Status"
  
  echo -e "${BLUE}Elastic Beanstalk Environments:${NC}"
  aws elasticbeanstalk describe-environments \
    --application-name minisocial \
    --region eu-central-1 \
    --query 'Environments[*].[EnvironmentName,Status,Health,CNAME]' \
    --output table 2>/dev/null || print_warning "No EU environments found"
  
  echo ""
  aws elasticbeanstalk describe-environments \
    --application-name minisocial \
    --region us-east-1 \
    --query 'Environments[*].[EnvironmentName,Status,Health,CNAME]' \
    --output table 2>/dev/null || print_warning "No US environments found"
  
  echo ""
  echo -e "${BLUE}Route 53 Configuration:${NC}"
  aws route53 list-resource-record-sets \
    --hosted-zone-id "$HOSTED_ZONE_ID" \
    --query "ResourceRecordSets[?Name=='$DOMAIN.']" \
    --output table 2>/dev/null || print_warning "No Route 53 records found"
  
  echo ""
}

# Function: Show help
show_help() {
  cat << EOF
${BLUE}MiniSocial Infrastructure Deployment${NC}

${GREEN}Usage:${NC}
  ./deploy.sh [command]

${GREEN}Commands:${NC}
  ${YELLOW}eu${NC}              Deploy to EU region (eu-central-1)
  ${YELLOW}us${NC}              Deploy to US region (us-east-1)
  ${YELLOW}all${NC}             Deploy to all regions
  ${YELLOW}route53${NC}         Setup Route 53 latency-based routing
  ${YELLOW}status${NC}          Show current infrastructure status
  ${YELLOW}help${NC}            Show this help message

${GREEN}Examples:${NC}
  # Deploy to EU only
  ./deploy.sh eu

  # Deploy to both regions
  ./deploy.sh all

  # Setup Route 53 after deployment
  ./deploy.sh route53

  # Check status
  ./deploy.sh status

${GREEN}Full Deployment Flow:${NC}
  1. ./deploy.sh eu          # Deploy EU backend
  2. ./deploy.sh route53     # Setup DNS
  3. ./deploy.sh us          # (Optional) Deploy US backend
  4. ./deploy.sh route53     # Update DNS for multi-region

${BLUE}Note:${NC} Make sure you have configured .env file with your secrets!

EOF
}

# Main script logic
case "${1:-help}" in
  eu)
    deploy_region "eu-central-1"
    print_info "Next step: ./deploy.sh route53"
    ;;
  us)
    deploy_region "us-east-1"
    print_info "Next step: ./deploy.sh route53"
    ;;
  all)
    deploy_region "eu-central-1"
    deploy_region "us-east-1"
    print_info "Next step: ./deploy.sh route53"
    ;;
  route53)
    setup_route53
    ;;
  status)
    show_status
    ;;
  help|--help|-h)
    show_help
    ;;
  *)
    print_error "Unknown command: $1"
    echo ""
    show_help
    exit 1
    ;;
esac
