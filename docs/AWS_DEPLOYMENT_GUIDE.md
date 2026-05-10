# Propertize AWS Deployment Guide

This guide covers end-to-end AWS deployment for all services in this workspace:
- Java services: api-gateway, auth-service, propertize, employee-service, payment-service, payroll-service, service-registry
- Python services: report-service, vendor-matching, document-service, search-reranker
- Workers: analytics-worker, payment-worker, screening-worker
- Frontend: propertize-front-end (Next.js)
- Data and messaging: PostgreSQL, MongoDB, Redis, Kafka, MinIO equivalent

## 1. Target AWS Architecture

Use managed services where possible:
- Compute: Amazon ECS (Fargate)
- Container registry: Amazon ECR
- Database (shared): Amazon RDS PostgreSQL
- MongoDB: Amazon DocumentDB (Mongo-compatible) or MongoDB Atlas on AWS
- Redis: Amazon ElastiCache for Redis
- Kafka: Amazon MSK
- Object storage: Amazon S3 (replace MinIO in production)
- Service discovery: ECS service discovery (Cloud Map) or keep service-registry in ECS
- Load balancing: ALB
- TLS and DNS: ACM + Route53
- Secrets: AWS Secrets Manager
- Logs and metrics: CloudWatch Logs + CloudWatch Alarms

## 2. Prerequisites

Install and configure:
1. AWS CLI v2
2. Docker
3. jq (optional)
4. Terraform or AWS CDK (optional but recommended)

Authenticate:
```bash
aws configure
aws sts get-caller-identity
```

Set region and account helpers:
```bash
export AWS_REGION=us-east-1
export AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
```

## 3. Create ECR Repositories

Create one repo per deployable service:
```bash
for repo in \
  api-gateway auth-service propertize employee-service payment-service payroll-service service-registry \
  report-service vendor-matching document-service search-reranker \
  analytics-worker payment-worker screening-worker \
  propertize-front-end
 do
  aws ecr create-repository --repository-name "$repo" --region "$AWS_REGION" || true
 done
```

Login to ECR:
```bash
aws ecr get-login-password --region "$AWS_REGION" \
| docker login --username AWS --password-stdin "$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com"
```

## 4. Build and Push Images

Run from repo root. Tag by git SHA:
```bash
export IMAGE_TAG=$(git rev-parse --short HEAD)
```

Example (repeat per service):
```bash
# api-gateway
cd api-gateway
docker build -t "$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/api-gateway:$IMAGE_TAG" .
docker push "$AWS_ACCOUNT_ID.dkr.ecr.$AWS_REGION.amazonaws.com/api-gateway:$IMAGE_TAG"
cd ..
```

Do the same for each directory that has a Dockerfile.

## 5. Provision AWS Infrastructure

Minimum production setup:
1. VPC across 2-3 AZs
2. Public subnets: ALB, NAT
3. Private subnets: ECS services, RDS, MSK, ElastiCache, DocumentDB
4. Security groups:
   - ALB ingress 80/443 from internet
   - ECS service ingress from ALB or internal SG only
   - RDS/Postgres ingress from ECS SG only
   - Redis ingress from ECS SG only
   - MSK ingress from ECS SG only
5. IAM roles:
   - ECS task execution role (pull ECR, write logs)
   - ECS task role (read Secrets Manager, S3)

Use Terraform/CDK to keep this reproducible.

## 6. Configure Data Services

### PostgreSQL (shared DB)
1. Create RDS PostgreSQL instance
2. Create database: `propertize_db`
3. Set creds in Secrets Manager
4. Update each service env:
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`

### Mongo-compatible
- Prefer DocumentDB for managed infra
- Update mongo connection env vars used by services

### Redis
- Create ElastiCache Redis cluster
- Set `REDIS_HOST`, `REDIS_PORT`

### Kafka
- Create MSK cluster
- Set bootstrap servers in env for publishers/consumers

### Object storage
- Replace MinIO usage with S3 in production env config
- Create bucket(s) and set IAM access for services

## 7. Secrets and Environment Variables

Store sensitive values in Secrets Manager:
- JWT keys and auth secrets
- DB credentials
- Stripe keys (if used)
- SMTP credentials
- Any third-party API keys

Pass secrets to ECS tasks via task definition `secrets`.

Pass non-secret config via task definition `environment`.

## 8. Deploy ECS Services

Create one ECS cluster for platform services (or split by domain).

Deploy order:
1. service-registry (if still required)
2. auth-service
3. core Java services (propertize, employee-service, payment-service, payroll-service)
4. python APIs
5. workers
6. api-gateway
7. frontend

For each service:
1. Create task definition with image + env + secrets
2. Create ECS service (Fargate)
3. Attach to target group if HTTP service
4. Set health check path:
   - Java: `/actuator/health`
   - Python: `/health`
5. Configure autoscaling policies

## 9. API Gateway and Routing on AWS

Keep external traffic through `api-gateway`.

ALB routing:
- Public listener 443 -> target group for `api-gateway`
- Internal service-to-service traffic stays private

If moving away from Eureka later, update service URLs/discovery strategy accordingly.

## 10. Frontend Deployment (Next.js)

Two AWS options:
1. ECS Fargate (containerized frontend)
2. Amplify Hosting (recommended for Next.js app hosting simplicity)

If ECS:
- Build/push `propertize-front-end` image
- Deploy behind ALB
- Set env vars for backend base URL to your gateway domain

If Amplify:
- Connect repo and branch
- Set build settings and env vars
- Point API URL to `https://<your-domain>/api/v1/...`

## 11. Domain, TLS, and DNS

1. Request ACM cert for your domain
2. Attach cert to ALB listener 443
3. Add Route53 alias record to ALB
4. Enforce HTTPS redirect

## 12. Observability and Operations

Enable:
1. CloudWatch logs for all ECS services
2. Alarms:
   - ECS task restarts
   - ALB 5xx rate
   - RDS CPU/storage/connections
   - MSK broker health
   - Redis memory/evictions
3. Optional tracing with AWS X-Ray or OpenTelemetry

## 13. CI/CD Recommended Flow

Use GitHub Actions:
1. Build images on push to main
2. Push images to ECR
3. Update ECS task definitions
4. Roll out ECS services
5. Run post-deploy smoke tests

Use deployment environments:
- dev
- staging
- prod

## 14. Production Hardening Checklist

1. No default credentials
2. Secrets only in Secrets Manager
3. Private subnets for services/data
4. Least-privilege IAM roles
5. ALB + WAF enabled
6. Backup policies for RDS/DocumentDB
7. Blue/green or canary for high-risk services
8. Runbook for rollback and incident response

## 15. Fly.io Removal Status

Fly.io deployment artifacts removed from this workspace:
- All `fly.toml` files removed
- `.github/workflows/deploy.yml` (Fly workflow) removed

## 16. Rollback Strategy

If deployment fails:
1. Revert ECS service to previous task definition revision
2. Keep database schema backward-compatible before app cutover
3. Roll back gateway last if backend compatibility is uncertain

Command pattern:
```bash
aws ecs update-service \
  --cluster <cluster> \
  --service <service> \
  --task-definition <previous-task-def-arn> \
  --force-new-deployment
```

## 17. First Production Cutover Plan

1. Deploy full stack in staging
2. Run migration and smoke tests
3. Validate auth flow and all critical routes
4. Freeze writes briefly (if needed)
5. Switch DNS to AWS ALB
6. Monitor for 30-60 minutes
7. Remove freeze and declare live

## 18. Production Without DNS (Immediate Bootstrap)

You can run production before owning DNS by using AWS ALB DNS names directly.

Important constraints:
1. Use HTTP ALB listeners first (port 80) because ACM public certs cannot be issued for `*.elb.amazonaws.com`.
2. When DNS is ready, move to HTTPS + ACM + Route53 and then update URLs.

Suggested temporary URL mapping:
1. Frontend URL: `http://<frontend-alb-dns>`
2. Gateway URL: `http://<api-gateway-alb-dns>`

Required env values for this no-DNS phase:

Frontend (`propertize-front-end`):
```env
NEXT_PUBLIC_API_URL=http://<api-gateway-alb-dns>
API_URL=http://api-gateway:8080
NEXTAUTH_URL=http://<frontend-alb-dns>
NEXTAUTH_URL_INTERNAL=http://127.0.0.1:3000
AUTH_URL=http://<frontend-alb-dns>
AUTH_TRUST_HOST=true
```

API Gateway:
```env
ALLOWED_ORIGINS=http://<frontend-alb-dns>
```

Notes:
1. Keep service-to-service traffic private inside VPC/security groups.
2. Only frontend and gateway ALBs should be public.
3. Once DNS is available, replace ALB DNS values with your domain values and enable HTTPS redirect.
