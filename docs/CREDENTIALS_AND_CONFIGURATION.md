# Propertize Platform - Credentials & Configuration

**Last Updated**: February 16, 2026  
**Environment**: Local Development

---

## 🔐 Database Credentials

### PostgreSQL (Local)

```
Host:     localhost
Port:     5432
Database: propertize_db
Username: dbuser
Password: dbpassword

Connection String:
jdbc:postgresql://localhost:5432/propertize_db

CLI Access:
psql -h localhost -U dbuser -d propertize_db
```

### MongoDB (Docker)

```
Host:     localhost
Port:     27017
Database: propertize_db
Username: admin
Password: mongo_secure_pass

Connection String:
mongodb://admin:mongo_secure_pass@localhost:27017/propertize_db?authSource=admin

CLI Access:
mongosh "mongodb://admin:mongo_secure_pass@localhost:27017/propertize_db?authSource=admin"
```

---

## 🔒 Cache & Session Credentials

### Redis (Docker)

```
Host:     localhost
Port:     6379
Password: redis_secure_pass

CLI Access:
redis-cli -h localhost -p 6379 -a redis_secure_pass

Test Connection:
redis-cli -h localhost -p 6379 -a redis_secure_pass PING
```

---

## 📨 Message Broker

### Kafka (Docker)

```
Bootstrap Servers: localhost:9092
Zookeeper:        localhost:2181

No authentication required for local development
```

---

## 🌐 Service Registry

### Eureka Dashboard

```
URL:      http://localhost:8761
Username: admin
Password: admin

Direct Access:
http://admin:admin@localhost:8761
```

---

## 🔑 JWT Configuration

### RSA Keys Location

```
Private Key: ./auth-service/keys/private_key.pem
Public Key:  ./auth-service/keys/public_key.pem

These keys are shared across all services for JWT token verification
```

### JWT Secret (Fallback)

```
JWT_SECRET=default-secret
(Only used if RSA keys are not available)
```

---

## 🛡️ Service-to-Service Authentication

### API Keys

```
Propertize Service API Key:
propertize-secret-key-12345

Auth Service API Key:
auth-service-secret-key-12345

Header Name: X-Service-Api-Key
Service Identifier: X-Service-Name
```

**Usage Example**:

```bash
curl -H "X-Service-Api-Key: auth-service-secret-key-12345" \
     -H "X-Service-Name: auth-service" \
     http://localhost:8082/api/v1/internal/users
```

---

## 🔐 NextAuth Configuration

### Frontend Authentication

```
NEXTAUTH_URL:    http://localhost:3000
NEXTAUTH_SECRET: s9cJk2MzOgFNXIztkLv4ki1vFMZx0o43QefbvwBOTM8=
AUTH_SECRET:     s9cJk2MzOgFNXIztkLv4ki1vFMZx0o43QefbvwBOTM8=
AUTH_TRUST_HOST: true
```

---

## 🖥️ Management UI Credentials

### Mongo Express

```
URL:      http://localhost:8089
Username: admin
Password: admin

Basic Auth: admin/admin
```

### Kafka UI

```
URL: http://localhost:8090
No authentication required
```

---

## 🚀 Service Ports

| Service                   | Port  | URL                   |
| ------------------------- | ----- | --------------------- |
| **Databases**             |
| PostgreSQL                | 5432  | localhost:5432        |
| MongoDB                   | 27017 | localhost:27017       |
| **Infrastructure**        |
| Redis                     | 6379  | localhost:6379        |
| Kafka                     | 9092  | localhost:9092        |
| Zookeeper                 | 2181  | localhost:2181        |
| **Services**              |
| Service Registry (Eureka) | 8761  | http://localhost:8761 |
| Auth Service              | 8081  | http://localhost:8081 |
| Propertize Main           | 8082  | http://localhost:8082 |
| Employee Service          | 8083  | http://localhost:8083 |
| API Gateway               | 8080  | http://localhost:8080 |
| Frontend                  | 3000  | http://localhost:3000 |
| **Management UIs**        |
| Mongo Express             | 8089  | http://localhost:8089 |
| Kafka UI                  | 8090  | http://localhost:8090 |

---

## 📁 Configuration Files

### Application Profiles

All services use the **local** profile:

```
service-registry/src/main/resources/application-local.yml
auth-service/src/main/resources/application-local.yml
propertize/src/main/resources/application-local.yml
employee-service/src/main/resources/application-local.yml
api-gateway/src/main/resources/application-local.yml
propertize-front-end/.env.local
```

### Environment Variables

Root directory `.env` file:

```bash
# Database
DB_USERNAME=dbuser
DB_PASSWORD=dbpassword
DB_HOST=localhost
DB_PORT=5432
DB_NAME=propertize_db

# MongoDB (Docker)
MONGO_USERNAME=admin
MONGO_PASSWORD=mongo_secure_pass

# Redis (Docker)
REDIS_PASSWORD=redis_secure_pass

# JWT
JWT_SECRET=default-secret

# Service Auth
PROPERTIZE_SERVICE_API_KEY=propertize-secret-key-12345
AUTH_SERVICE_API_KEY=auth-service-secret-key-12345

# Application
SPRING_PROFILES_ACTIVE=local
NODE_ENV=development
```

---

## 🧪 Test Credentials

### Default Admin User

```
Username: admin
Password: admin123
Email:    admin@propertize.com
Roles:    ADMIN, SUPER_ADMIN
```

### Sample Organization Owner

```
Username: owner@acme.com
Password: owner123
Organization: ACME Properties
```

### Sample Tenant

```
Username: tenant@example.com
Password: tenant123
Role: TENANT
```

---

## 🔧 Health Check Endpoints

### Service Health

```bash
# Service Registry
curl http://localhost:8761/actuator/health

# Auth Service
curl http://localhost:8081/actuator/health

# Propertize Main
curl http://localhost:8082/actuator/health

# Employee Service
curl http://localhost:8083/actuator/health

# API Gateway
curl http://localhost:8080/actuator/health

# Frontend
curl http://localhost:3000
```

### Infrastructure Health

```bash
# PostgreSQL
psql -h localhost -U dbuser -d propertize_db -c "SELECT 1;"

# MongoDB
mongosh "mongodb://admin:mongo_secure_pass@localhost:27017/admin" --eval "db.adminCommand('ping')"

# Redis
redis-cli -h localhost -p 6379 -a redis_secure_pass PING

# Kafka
docker exec propertize-kafka kafka-broker-api-versions --bootstrap-server localhost:9092
```

---

## 🔐 Security Notes

### ⚠️ Development Only

**IMPORTANT**: These credentials are for **LOCAL DEVELOPMENT ONLY**

**DO NOT USE IN PRODUCTION**

### Production Checklist

Before deploying to production:

- [ ] Change all default passwords
- [ ] Generate new JWT RSA keys (2048-bit minimum)
- [ ] Use strong, unique service API keys (32+ characters)
- [ ] Enable HTTPS and set `secure: true` for cookies
- [ ] Use environment-specific secrets (Azure Key Vault, AWS Secrets Manager)
- [ ] Enable Redis authentication with strong password
- [ ] Configure Kafka SASL authentication
- [ ] Enable MongoDB authentication and use unique passwords
- [ ] Set up proper database user permissions (least privilege)
- [ ] Rotate secrets regularly
- [ ] Enable audit logging
- [ ] Configure rate limiting
- [ ] Set up WAF (Web Application Firewall)

---

## 🚀 Quick Start Commands

### Start Infrastructure

```bash
docker-compose -f docker-compose.infra.yml up -d
```

### Setup PostgreSQL

```bash
./setup-postgres-local.sh
```

### Start All Services

```bash
./start-all-local.sh
```

### Stop All Services

```bash
./stop-all-local.sh
```

### Test API Authentication

```bash
# Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'

# Use token in subsequent requests
curl -H "Authorization: Bearer <your-token>" \
     http://localhost:8080/api/v1/organizations
```

---

## 📊 Monitoring & Observability

### Prometheus Metrics

```
Available at: /actuator/prometheus on each service

Propertize:       http://localhost:8082/actuator/prometheus
Auth Service:     http://localhost:8081/actuator/prometheus
Employee Service: http://localhost:8083/actuator/prometheus
API Gateway:      http://localhost:8080/actuator/prometheus
```

### Application Info

```
Available at: /actuator/info on each service

Example: http://localhost:8082/actuator/info
```

### Service Registry Dashboard

```
View all registered services:
http://localhost:8761

Check service instances, health status, and metadata
```

---

## 🐛 Troubleshooting

### Can't Connect to PostgreSQL

```bash
# Check if running
pg_isready

# Start PostgreSQL
brew services start postgresql@16  # macOS
sudo systemctl start postgresql    # Linux

# Test connection
psql -h localhost -U dbuser -d propertize_db
```

### Redis Connection Refused

```bash
# Check Docker container
docker ps | grep redis

# View logs
docker logs propertize-redis

# Restart
docker-compose -f docker-compose.infra.yml restart redis
```

### Service Won't Start

```bash
# Check port availability
lsof -i :8081  # Replace with your port

# Kill process
kill -9 <PID>

# View logs
tail -f logs/service-name.log
```

### Eureka Registration Issues

```bash
# Verify Eureka is running
curl http://localhost:8761/actuator/health

# Check service logs for registration errors
tail -f logs/auth-service.log | grep eureka

# Wait 30 seconds for registration to complete
```

---

## 📝 Environment Variable Reference

### Required Variables

```bash
# Database
DB_USERNAME=dbuser
DB_PASSWORD=dbpassword
DB_NAME=propertize_db

# Redis
REDIS_PASSWORD=redis_secure_pass

# MongoDB
MONGO_USERNAME=admin
MONGO_PASSWORD=mongo_secure_pass

# JWT
JWT_SECRET=default-secret

# Service Auth
PROPERTIZE_SERVICE_API_KEY=propertize-secret-key-12345
AUTH_SERVICE_API_KEY=auth-service-secret-key-12345
```

### Optional Variables

```bash
# Tracing
TRACING_ENABLED=false
OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4317

# Logging
LOGGING_LEVEL_ROOT=INFO
LOGGING_LEVEL_COM_PROPERTIZE=DEBUG

# Session
SESSION_TIMEOUT=1h

# Cache
CACHE_TTL=300000
```

---

## 🔄 Credential Rotation

### How to Rotate Secrets

#### 1. Database Password

```bash
# PostgreSQL
psql -U postgres
ALTER USER dbuser WITH PASSWORD 'new-secure-password';

# Update in all application-local.yml files
# Restart services
```

#### 2. JWT Keys

```bash
cd auth-service/keys

# Generate new RSA key pair
openssl genrsa -out private_key.pem 2048
openssl rsa -in private_key.pem -pubout -out public_key.pem

# Copy to all services that need public key
# Restart all services
```

#### 3. Service API Keys

```bash
# Generate new key
openssl rand -base64 32

# Update in .env file
# Update in application-local.yml files
# Restart services
```

#### 4. NextAuth Secret

```bash
# Generate new secret
openssl rand -base64 32

# Update in propertize-front-end/.env.local
# Restart frontend
```

---

## 📞 Support

For issues or questions:

1. Check logs in `logs/` directory
2. Verify all credentials are correct
3. Ensure all infrastructure services are running
4. Check firewall/port availability
5. Review LOCAL_DEVELOPMENT_GUIDE.md for detailed setup

---

**Last Updated**: February 16, 2026  
**Version**: 1.0.0  
**Profile**: Local Development
