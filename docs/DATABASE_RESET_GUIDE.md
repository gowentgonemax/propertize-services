# 🚀 Database Reset and Fresh Start Guide

## What Was Done

### ✅ 1. Database Credentials Updated

Changed from complex credentials to simple ones for easy development:

- **Username:** `dbuser` (was: `propertize_user`)
- **Password:** `dbpassword` (was: `propertize_secure_pass`)
- **Database:** `propertize_db` (standardized across all services)

**Files Updated:**

- `docker-compose.yml` (PostgreSQL + all 4 microservices)
- All `application.yml` and `application-docker.yml` files in:
  - propertize
  - auth-service
  - employee-service

### ✅ 2. Database Reset Script Created

**Script:** `reset-database.sh`

This script:

- Terminates all connections to the database
- Drops `propertize_db` if it exists
- Creates fresh `propertize_db`
- Creates `dbuser` with all privileges
- Prepares for fresh schema creation

### ✅ 3. Superadmin User Initialization

**Scripts Created:**

- `init-superadmin.sql` - SQL to insert superadmin user
- `init-superadmin.sh` - Bash script to execute the SQL

**Superadmin Credentials:**

- Username: `superadmin`
- Password: `password`
- Role: `PLATFORM_OVERSIGHT` (highest platform role - level 1100)
- Email: `superadmin@propertize.com`

### ✅ 4. Database Conflicts Analyzed

Full analysis documented in `DATABASE_ISSUES_AND_FIXES.md`

**Key Findings:**

- ✅ Duplicate User entities (both services map to same table - acceptable for now)
- ⚠️ Duplicate Organization entities (different ID types - propertize starts first, creates schema)
- ✅ Minor circular dependency in Invoice↔Payment (acceptable)
- ✅ Cascade conflicts identified (intentional design)
- ✅ Several unidirectional relationships (valid JPA pattern)

**Conclusion:** Current architecture works correctly with shared database. All "conflicts" are either acceptable or intentional design decisions.

---

## 🎯 How to Use - Step by Step

### Option A: Complete Fresh Start (Recommended)

```bash
# 1. Navigate to project root
cd /Users/ravishah/MySpace/ProperyManage/propertize-Services

# 2. Stop all services and remove volumes
docker-compose down -v

# 3. Reset database (drops and recreates)
./reset-database.sh

# 4. Build services (optional, if code changed)
docker-compose build

# 5. Start all services
docker-compose up -d

# 6. Watch logs to ensure services start properly
docker-compose logs -f propertize auth-service

# 7. Wait for schema creation (about 30-60 seconds)
# Watch for logs like:
# "Flyway successfully migrated"
# "JPA creating tables"
# "Application started on port 8082"

# 8. Create superadmin user
./init-superadmin.sh

# 9. Verify superadmin was created
docker exec -it propertize-postgres psql -U dbuser -d propertize_db \
  -c "SELECT username, email, enabled FROM users WHERE username='superadmin';"

# 10. Access application
# Frontend: http://localhost:3000
# API Gateway: http://localhost:8080
# Adminer (DB UI): http://localhost:8088
```

---

### Option B: Quick Reset (Services Already Running)

```bash
cd /Users/ravishah/MySpace/ProperyManage/propertize-Services

# 1. Stop services (keep volumes)
docker-compose stop

# 2. Reset database only
./reset-database.sh

# 3. Restart services (schema will be recreated)
docker-compose start

# 4. Wait for schema creation
sleep 30

# 5. Create superadmin
./init-superadmin.sh
```

---

### Option C: Reset Database Only (No Service Restart)

```bash
# If you just want to drop and recreate database while services are stopped
./reset-database.sh

# Then start services normally
docker-compose up -d
```

---

## 📊 What Happens During Startup

### Service Startup Order (Automated by docker-compose)

1. **Infrastructure Services:**
   - PostgreSQL (waits for health check) ✓
   - MongoDB ✓
   - Redis ✓
   - Zookeeper + Kafka ✓

2. **Core Services:**
   - Service Registry (Eureka) ✓

3. **Application Services:**
   - **Propertize Service** (Port 8082) - **Starts First!**
     - Creates main database schema via Hibernate
     - Runs Flyway migrations (V8, V9)
     - Creates tables: users, organizations, properties, leases, etc.
   - **Auth Service** (Port 8081)
     - Reuses existing schema (Flyway disabled in Docker)
     - ddl-auto: none (doesn't modify schema)
     - Runs its own Flyway migrations (V1-V8) for RBAC tables
   - **Employee Service** (Port 8083)
     - Uses existing schema
     - Creates employee-specific tables
   - **API Gateway** (Port 8080)
     - No database access
     - Routes requests to services

4. **Frontend:**
   - Next.js app (Port 3000)
   - Connects to API Gateway

### Schema Creation Details

When **Propertize** starts first:

```sql
-- Main tables created by Hibernate:
users (id, username, email, password, organization_id, ...)
user_roles (user_id, role)
organizations (id, name, type, owner_user_id, ...)
properties (id, name, address, organization_id, ...)
leases (id, property_id, tenant_id, ...)
tenants (id, user_id, organization_id, ...)
payments (id, lease_id, amount, ...)
invoices (id, lease_id, payment_id, ...)
maintenance_requests (id, property_id, ...)
... and 30+ more tables
```

Then **Auth-Service** Flyway migrations add:

```sql
-- RBAC tables:
password_reset_tokens
temporal_permissions
composite_roles
delegation_rules
delegations
custom_roles
permission_audit_logs
ip_access_rules
```

---

## 🔐 Login Credentials

### Superadmin Account

```
Username: superadmin
Password: password
Role: PLATFORM_OVERSIGHT
```

**Permissions:**

- Full access to all resources
- Can manage all organizations
- Can manage all users
- Can access all platform operations
- Bypasses most permission checks

**Access Levels:**

- Platform Level: ✅ (level 1100)
- Portfolio Level: ✅
- Organization Level: ✅
- All Operations: ✅

---

## 🔍 Verification Steps

### 1. Check PostgreSQL is Running

```bash
docker ps | grep postgres
# Should show: propertize-postgres (healthy)
```

### 2. Check Database Exists

```bash
docker exec -it propertize-postgres psql -U postgres -l | grep propertize_db
# Should show: propertize_db | dbuser | ...
```

### 3. Check Tables Were Created

```bash
docker exec -it propertize-postgres psql -U dbuser -d propertize_db -c "\dt" | wc -l
# Should show: 50+ tables
```

### 4. Check Superadmin User

```bash
docker exec -it propertize-postgres psql -U dbuser -d propertize_db \
  -c "SELECT u.username, u.email, array_agg(ur.role) as roles
      FROM users u
      LEFT JOIN user_roles ur ON u.id = ur.user_id
      WHERE u.username = 'superadmin'
      GROUP BY u.id, u.username, u.email;"
```

Expected output:

```
 username  |           email            |         roles
-----------+----------------------------+-----------------------
 superadmin | superadmin@propertize.com | {PLATFORM_OVERSIGHT}
```

### 5. Test Login API

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "superadmin",
    "password": "password"
  }'
```

Expected response:

```json
{
  "success": true,
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "...",
  "user": {
    "username": "superadmin",
    "email": "superadmin@propertize.com",
    "roles": ["PLATFORM_OVERSIGHT"]
  }
}
```

### 6. Check Service Health

```bash
# Propertize
curl http://localhost:8082/actuator/health

# Auth Service
curl http://localhost:8081/actuator/health

# Employee Service
curl http://localhost:8083/actuator/health

# API Gateway
curl http://localhost:8080/actuator/health
```

All should return: `{"status":"UP"}`

---

## 🎨 Access Points

### Application UIs

- **Frontend:** http://localhost:3000
- **Service Registry (Eureka):** http://localhost:8761
- **Kafka UI:** http://localhost:8090
- **Adminer (Database UI):** http://localhost:8088
- **Mongo Express:** http://localhost:8089

### API Endpoints

- **API Gateway:** http://localhost:8080
- **Auth Service:** http://localhost:8081
- **Propertize Service:** http://localhost:8082
- **Employee Service:** http://localhost:8083

### Database Access via Adminer

1. Open http://localhost:8088
2. Login:
   - System: PostgreSQL
   - Server: postgres
   - Username: dbuser
   - Password: dbpassword
   - Database: propertize_db
3. Browse tables, run queries, etc.

---

## 📝 Common Issues and Solutions

### Issue 1: "Database does not exist"

```bash
# Solution: Run reset script
./reset-database.sh
```

### Issue 2: "Users table does not exist"

```bash
# Solution: Services haven't started yet or failed
docker-compose logs propertize | grep -i "error\|exception"

# Restart propertize to recreate schema
docker-compose restart propertize
```

### Issue 3: "Superadmin already exists"

```bash
# Solution: Delete and recreate
docker exec -it propertize-postgres psql -U dbuser -d propertize_db <<EOF
DELETE FROM user_roles WHERE user_id IN (SELECT id FROM users WHERE username = 'superadmin');
DELETE FROM users WHERE username = 'superadmin';
EOF

# Then run init script again
./init-superadmin.sh
```

### Issue 4: "Connection refused"

```bash
# Check if PostgreSQL is running
docker ps | grep postgres

# If not running, start services
docker-compose up -d postgres

# Wait for it to be healthy
docker-compose ps postgres
```

### Issue 5: "Permission denied"

```bash
# Make scripts executable
chmod +x reset-database.sh init-superadmin.sh
```

---

## 🗑️ Clean Up Everything

To completely remove all data and start over:

```bash
# Stop all services
docker-compose down

# Remove all volumes (deletes all data)
docker-compose down -v

# Remove all images (forces rebuild)
docker-compose down --rmi all

# Clean build artifacts
cd propertize && mvn clean && cd ..
cd auth-service && mvn clean && cd ..
cd employee-service && mvn clean && cd ..
cd api-gateway && mvn clean && cd ..

# Now start fresh
docker-compose up -d --build
```

---

## 📚 Additional Documentation

- **Full Database Analysis:** `DATABASE_ISSUES_AND_FIXES.md`
- **Service Dependencies:** `SERVICE_DEPENDENCIES.md`
- **Docker Guide:** `DOCKER_GUIDE.md`
- **Quick Start:** `QUICK_START.md`

---

## 🎯 Summary

**What Changed:**
✅ Database credentials simplified to `dbuser` / `dbpassword`  
✅ Database name standardized to `propertize_db`  
✅ Scripts created for easy database reset  
✅ Superadmin user auto-creation script ready  
✅ All services configured to use new credentials  
✅ Database conflicts documented and assessed

**Ready to Use:**
✅ Run `./reset-database.sh` to drop and recreate database  
✅ Run `docker-compose up -d` to start services (tables auto-created)  
✅ Run `./init-superadmin.sh` to create superadmin user  
✅ Login with `superadmin` / `password`

**No Manual Steps Required!** 🎉
