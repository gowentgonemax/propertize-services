#!/bin/bash

# ============================================
# Database Reset Script
# Drops and recreates the propertize_db database
# ============================================

set -e  # Exit on error

echo "============================================"
echo "  Propertize Database Reset Script"
echo "============================================"
echo ""

# Configuration
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="propertize_db"
DB_USER="dbuser"
DB_PASSWORD="dbpassword"
POSTGRES_USER="${POSTGRES_USER:-postgres}"
POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-postgres}"

# Check if PostgreSQL is running
if ! docker ps --filter "name=propertize-postgres" --filter "status=running" | grep -q propertize-postgres; then
    echo "⚠️  PostgreSQL container is not running!"
    echo "Starting PostgreSQL container..."
    docker-compose up -d postgres
    echo "Waiting for PostgreSQL to be ready..."
    sleep 10
fi

echo "📊 Dropping existing database..."
# Connect as postgres superuser to drop and recreate database
docker exec -e PGPASSWORD="${POSTGRES_PASSWORD}" propertize-postgres psql -U postgres -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '${DB_NAME}' AND pid <> pg_backend_pid();" 2>/dev/null || true

docker exec -e PGPASSWORD="${POSTGRES_PASSWORD}" propertize-postgres psql -U postgres -c "DROP DATABASE IF EXISTS ${DB_NAME};" 2>/dev/null || true

echo "✅ Database dropped successfully"

echo "🆕 Creating new database..."
docker exec -e PGPASSWORD="${POSTGRES_PASSWORD}" propertize-postgres psql -U postgres -c "CREATE DATABASE ${DB_NAME};"

echo "👤 Creating database user if not exists..."
docker exec -e PGPASSWORD="${POSTGRES_PASSWORD}" propertize-postgres psql -U postgres -c "DO \$\$ BEGIN IF NOT EXISTS (SELECT FROM pg_user WHERE usename = '${DB_USER}') THEN CREATE USER ${DB_USER} WITH PASSWORD '${DB_PASSWORD}'; END IF; END \$\$;"

echo "🔐 Granting privileges..."
docker exec -e PGPASSWORD="${POSTGRES_PASSWORD}" propertize-postgres psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE ${DB_NAME} TO ${DB_USER};"
docker exec -e PGPASSWORD="${POSTGRES_PASSWORD}" propertize-postgres psql -U postgres -d ${DB_NAME} -c "GRANT ALL ON SCHEMA public TO ${DB_USER};"
docker exec -e PGPASSWORD="${POSTGRES_PASSWORD}" propertize-postgres psql -U postgres -d ${DB_NAME} -c "ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO ${DB_USER};"
docker exec -e PGPASSWORD="${POSTGRES_PASSWORD}" propertize-postgres psql -U postgres -d ${DB_NAME} -c "ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO ${DB_USER};"

echo ""
echo "============================================"
echo "✅ Database reset completed successfully!"
echo "============================================"
echo ""
echo "Database: ${DB_NAME}"
echo "User: ${DB_USER}"
echo "Password: ${DB_PASSWORD}"
echo "Host: ${DB_HOST}:${DB_PORT}"
echo ""
echo "📝 Next steps:"
echo "1. Start the services to auto-create tables:"
echo "   docker-compose up -d"
echo "2. Or run: ./init-superadmin.sh to create superadmin user"
echo ""
