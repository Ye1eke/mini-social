# MiniSocial Infrastructure

Infrastructure as Code (IaC) for MiniSocial using AWS CDK.

## Overview

This directory contains a simplified CDK setup that creates an Elastic Beanstalk environment with:

- Application Load Balancer (ALB) with security groups
- EC2 instances with proper IAM roles
- HTTPS support (optional, via ACM certificate)
- Environment variables configuration

The infrastructure uses the **default VPC** to minimize costs and complexity for educational purposes.

## Prerequisites

- Node.js 18+ and npm
- AWS CLI configured with credentials
- AWS CDK CLI: `npm install -g aws-cdk`

## Configuration

Edit `cdk.json` to configure your backend:

```json
{
  "context": {
    "backend": {
      "applicationName": "minisocial",
      "environmentName": "minisocial-backend-cdk",
      "solutionStackName": "64bit Amazon Linux 2023 v4.3.0 running Corretto 17",
      "certificateArn": "",
      "appPort": "8080",
      "env": {
        "B2_ENDPOINT": "https://s3.us-west-000.backblazeb2.com",
        "B2_ACCESS_KEY_ID": "placeholder-key-id",
        "B2_SECRET_ACCESS_KEY": "placeholder-secret-key",
        "B2_BUCKET_NAME": "minisocial-media"
      }
    }
  }
}
```

## Commands

```bash
# Install dependencies
npm install

# Synthesize CloudFormation template (show what will be created)
cdk synth

# Bootstrap CDK (one-time per account/region)
cdk bootstrap

# Deploy the stack
cdk deploy

# Show differences between deployed and local
cdk diff

# Destroy the stack
cdk destroy
```

## What Gets Created

- **Elastic Beanstalk Application**: Container for your environments
- **Elastic Beanstalk Environment**: Load-balanced environment with ALB
- **Security Groups**:
  - ALB SG: Allows HTTP (80) and HTTPS (443) from internet
  - Instance SG: Allows traffic only from ALB on port 8080
- **IAM Role & Instance Profile**: For EC2 instances with necessary permissions
- **CloudFormation Stack**: All resources managed as code

## Deployment Workflow

1. **CDK creates/updates infrastructure** (this repo)
2. **GitHub Actions deploys application** (JAR file to Beanstalk)

This separation is intentional - CDK manages infrastructure, while CI/CD handles application deployments.

## Cost Optimization

- Uses default VPC (no NAT Gateway costs)
- Single AZ deployment for dev
- t3.micro instances
- No separate RDS/ElastiCache (can be added via EB options if needed)

## Adding Database/Cache

You can add RDS and ElastiCache through Elastic Beanstalk options without creating separate stacks. Add to `optionSettings` in `minisocial-stack.ts`:

```typescript
// RDS PostgreSQL
{ namespace: "aws:rds:dbinstance", optionName: "DBEngine", value: "postgres" },
{ namespace: "aws:rds:dbinstance", optionName: "DBInstanceClass", value: "db.t3.micro" },

// ElastiCache Redis
{ namespace: "aws:elasticache", optionName: "CacheNodeType", value: "cache.t3.micro" },
```

## Troubleshooting

- **VPC lookup fails**: Ensure you have a default VPC in your region
- **Bootstrap required**: Run `cdk bootstrap` first
- **Permission errors**: Check your AWS credentials have necessary permissions
