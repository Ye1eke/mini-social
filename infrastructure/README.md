# MiniSocial Infrastructure

Infrastructure as Code for MiniSocial using AWS CDK - deploys to multiple regions with Elastic Beanstalk.

## Quick Start

```bash
# 1. Install dependencies
npm install

# 2. Configure secrets (edit with your values)
cp .env.example .env
nano .env

# 3. Deploy to EU region
cdk deploy MiniSocialBackendEb-eucentral1

# 4. (Optional) Deploy to US region
cdk deploy MiniSocialBackendEb-useast1
```

---

## What Gets Deployed

### Single Region (EU)

- **Elastic Beanstalk** environment with Auto Scaling
- **Application Load Balancer** (ALB)
- **EC2 instances** running your Spring Boot app
- **IAM roles** with proper permissions
- **Security groups** for network isolation

### Multi-Region (EU + US)

Same as above, but in both:

- `eu-central-1` (Frankfurt) - for European users
- `us-east-1` (N. Virginia) - for American users

**Cost:** ~$48/month (single) or ~$85/month (multi-region)

---

## Configuration

### 1. Secrets (.env file)

```bash
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://your-db.rds.amazonaws.com:5432/postgres
DB_USERNAME=minisocial
DB_PASSWORD=your-password

# JWT Secret (generate: openssl rand -base64 32)
JWT_SECRET=your-secret-here

# Backblaze B2 (optional)
B2_ENDPOINT=https://s3.us-west-000.backblazeb2.com
B2_ACCESS_KEY_ID=your-key
B2_SECRET_ACCESS_KEY=your-secret
B2_BUCKET_NAME=your-bucket

# Server Port
SERVER_PORT=5000
```

**Important:** `.env` is gitignored - your secrets stay local!

### 2. Regions (cdk.json)

```json
{
  "regions": ["eu-central-1", "us-east-1"],
  "primaryRegion": "eu-central-1"
}
```

Add/remove regions as needed.

---

## Deployment

### Prerequisites

```bash
# Install AWS CDK
npm install -g aws-cdk

# Configure AWS credentials
aws configure

# Bootstrap CDK (one-time per region)
cdk bootstrap aws://ACCOUNT_ID/eu-central-1
cdk bootstrap aws://ACCOUNT_ID/us-east-1
```

### Deploy Infrastructure

```bash
# Deploy to specific region
cdk deploy MiniSocialBackendEb-eucentral1

# Deploy to all regions
cdk deploy --all

# Preview changes (dry run)
cdk diff

# Destroy infrastructure
cdk destroy MiniSocialBackendEb-eucentral1
```

### Deploy Application

After infrastructure is ready, deploy your JAR:

**Via GitHub Actions** (automatic):

- Push to `main` branch
- GitHub Actions builds and deploys

**Manual deployment:**

```bash
cd backend
mvn clean package

# Create bundle
mkdir eb-bundle
cp target/*.jar eb-bundle/server.jar
echo "web: java -jar server.jar" > eb-bundle/Procfile
cd eb-bundle && zip -r ../deploy.zip ./*

# Deploy
aws elasticbeanstalk create-application-version \
  --application-name minisocial \
  --version-label v1 \
  --source-bundle S3Bucket="elasticbeanstalk-eu-central-1-ACCOUNT",S3Key="app.jar" \
  --region eu-central-1

aws elasticbeanstalk update-environment \
  --environment-name minisocial-backend-cdk-eucentral1 \
  --version-label v1 \
  --region eu-central-1
```

---

## Architecture

```
Internet
   ↓
Application Load Balancer (80/443)
   ↓
Auto Scaling Group (EC2 instances)
   ↓
Spring Boot App (port 5000)
   ↓
RDS PostgreSQL Database
```

**Multi-Region:**

```
Users → Route 53 (geo-routing) → Nearest Region → Your App → Shared Database
```

---

## Database Setup

### Option 1: Shared Database (Current)

Both regions connect to same database in eu-central-1.

**Pros:** Simple, no sync issues  
**Cons:** Higher latency for US users

### Option 2: Database Replication

Create read replica in us-east-1:

```bash
aws rds create-db-instance-read-replica \
  --db-instance-identifier minisocial-db-us \
  --source-db-instance-identifier arn:aws:rds:eu-central-1:ACCOUNT:db:minisocial-db \
  --region us-east-1
```

### Security Groups

Allow EB environments to access database:

```bash
# Get EB security group
SG=$(aws elasticbeanstalk describe-environment-resources \
  --environment-name minisocial-backend-cdk-eucentral1 \
  --region eu-central-1 \
  --query 'EnvironmentResources.Instances[0].Id' \
  --output text | xargs -I {} aws ec2 describe-instances \
  --instance-ids {} \
  --region eu-central-1 \
  --query 'Reservations[0].Instances[0].SecurityGroups[0].GroupId' \
  --output text)

# Allow access to database
aws ec2 authorize-security-group-ingress \
  --group-id YOUR_DB_SECURITY_GROUP \
  --protocol tcp \
  --port 5432 \
  --source-group $SG \
  --region eu-central-1
```

---

## Monitoring

### Check Environment Health

```bash
# EU region
aws elasticbeanstalk describe-environment-health \
  --environment-name minisocial-backend-cdk-eucentral1 \
  --attribute-names All \
  --region eu-central-1

# US region
aws elasticbeanstalk describe-environment-health \
  --environment-name minisocial-backend-cdk-useast1 \
  --attribute-names All \
  --region us-east-1
```

### View Logs

```bash
aws elasticbeanstalk describe-events \
  --environment-name minisocial-backend-cdk-eucentral1 \
  --region eu-central-1 \
  --max-items 20
```

### Get Environment URL

```bash
aws elasticbeanstalk describe-environments \
  --environment-names minisocial-backend-cdk-eucentral1 \
  --region eu-central-1 \
  --query 'Environments[0].CNAME' \
  --output text
```

---

## Troubleshooting

### 502 Bad Gateway

**Causes:**

1. App not running on correct port (check SERVER_PORT=5000)
2. Database connection failed (check security groups)
3. Application crashed (check logs)

**Fix:**

```bash
# Check logs
aws elasticbeanstalk describe-events \
  --environment-name minisocial-backend-cdk-eucentral1 \
  --region eu-central-1 \
  --max-items 20

# Check environment variables
aws elasticbeanstalk describe-configuration-settings \
  --environment-name minisocial-backend-cdk-eucentral1 \
  --application-name minisocial \
  --region eu-central-1 \
  --query 'ConfigurationSettings[0].OptionSettings[?Namespace==`aws:elasticbeanstalk:application:environment`]'
```

### Health Check Failing

Ensure your app has a health endpoint at `/` or `/health`:

```java
@RestController
public class HealthController {
    @GetMapping("/")
    public Map<String, String> root() {
        return Map.of("status", "ok");
    }
}
```

### Database Connection Failed

1. Check security group allows EB → RDS
2. Verify database endpoint in environment variables
3. Ensure database is publicly accessible or in same VPC

---

## GitHub Actions

Update `.github/workflows/backend-eb.yml`:

```yaml
environment_name: minisocial-backend-cdk-eucentral1  # EU
# or
environment_name: minisocial-backend-cdk-useast1     # US
```

For multi-region, use matrix strategy:

```yaml
strategy:
  matrix:
    region:
      - name: eu-central-1
        env: minisocial-backend-cdk-eucentral1
      - name: us-east-1
        env: minisocial-backend-cdk-useast1
```

---

## Useful Commands

```bash
# List all stacks
cdk list

# Show what will change
cdk diff

# Generate CloudFormation template
cdk synth

# Deploy with auto-approval
cdk deploy --all --require-approval never

# Destroy everything
cdk destroy --all

# Check CDK version
cdk --version

# Update CDK
npm install -g aws-cdk
```

---

## Project Structure

```
infrastructure/
├── bin/
│   └── minisocial-cdk.ts       # Entry point, multi-region setup
├── lib/
│   └── minisocial-stack.ts     # Main stack definition
├── .env                         # Secrets (gitignored)
├── .env.example                 # Template
├── cdk.json                     # CDK configuration
├── package.json                 # Dependencies
└── README.md                    # This file
```

---

## Cost Optimization

**Single Region:**

- EC2 t3.micro: ~$7/month
- ALB: ~$16/month
- RDS db.t4g.micro: ~$12/month
- **Total: ~$35-48/month**

**Multi-Region:**

- 2× EC2: ~$14/month
- 2× ALB: ~$32/month
- 1× RDS: ~$12/month
- Data transfer: ~$5-10/month
- **Total: ~$63-85/month**

**To reduce costs:**

- Use AWS Free Tier (12 months)
- Stop environments when not in use: `cdk destroy`
- Use smaller instance types
- Delete unused resources

---

## Security Best Practices

1. **Never commit secrets** - Use `.env` (gitignored)
2. **Rotate credentials** regularly
3. **Use IAM roles** instead of access keys
4. **Enable MFA** on AWS account
5. **Review security groups** - least privilege
6. **Use HTTPS** - Add ACM certificate
7. **Monitor access** - Enable CloudTrail

---

## Next Steps

1. ✅ Deploy infrastructure
2. ⏳ Deploy application
3. ⏳ Test endpoints
4. ⏳ Set up custom domain (optional)
5. ⏳ Add database replication (optional)
6. ⏳ Configure monitoring/alerts
7. ⏳ Set up CI/CD for multi-region

---

## Support

- **AWS CDK Docs:** https://docs.aws.amazon.com/cdk/
- **Elastic Beanstalk Docs:** https://docs.aws.amazon.com/elasticbeanstalk/
- **Issues:** Check CloudFormation console for detailed errors

---

**Built with AWS CDK** | **Infrastructure as Code** | **Multi-Region Ready**
