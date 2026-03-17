#!/bin/bash

# ============================================
# PostgreSQL Local Setup Script
# ============================================

set -e

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE}PostgreSQL Local Setup for Propertize${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""

# Database credentials
DB_NAME="propertize_db"
DB_USER="dbuser"
DB_PASSWORD="dbpassword"

# Check if PostgreSQL is installed
if ! command -v psql &> /dev/null; then
    echo -e "${RED}✗ PostgreSQL is not installed${NC}"
    echo ""
    echo "Install PostgreSQL:"
    echo "  macOS:   brew install postgresql@16"
    echo "  Ubuntu:  sudo apt install postgresql-16"
    echo "  Windows: Download from https://www.postgresql.org/download/windows/"
    exit 1
fi

echo -e "${GREEN}✓${NC} PostgreSQL is installed"
psql --version

# Check if PostgreSQL is running
if ! pg_isready > /dev/null 2>&1; then
    echo -e "${YELLOW}PostgreSQL is not running. Starting...${NC}"
    
    # Try to start based on OS
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS
        brew services start postgresql@16
    elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
        # Linux
        sudo systemctl start postgresql
    else
        echo -e "${RED}Please start PostgreSQL manually${NC}"
        exit 1
    fi
    
    sleep 3
fi

if pg_isready > /dev/null 2>&1; then
    echo -e "${GREEN}✓${NC} PostgreSQL is running"
else
    echo -e "${RED}✗ Failed to start PostgreSQL${NC}"
    exit 1
fi

# Connect as default user and create database/user
echo ""
echo -e "${BLUE}Creating database and user...${NC}"

# Check if running as postgres user or default user
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS - use current user
    PSQL_CMD="psql postgres"
elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
    # Linux - use postgres user
    PSQL_CMD="sudo -u postgres psql"
else
    PSQL_CMD="psql -U postgres"
fi

# Create user if doesn't exist
$PSQL_CMD <<EOF
DO \$\$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_user WHERE usename = '$DB_USER') THEN
        CREATE USER $DB_USER WITH PASSWORD '$DB_PASSWORD';
        RAISE NOTICE 'User $DB_USER created';
    ELSE
        RAISE NOTICE 'User $DB_USER already exists';
    END IF;
END
\$\$;
EOF

# Create database if doesn't exist
$PSQL_CMD <<EOF
DO \$\$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_database WHERE datname = '$DB_NAME') THEN
        CREATE DATABASE $DB_NAME OWNER $DB_USER;
        RAISE NOTICE 'Database $DB_NAME created';
    ELSE
        RAISE NOTICE 'Database $DB_NAME already exists';
    END IF;
END
\$\$;
EOF

# Grant privileges
$PSQL_CMD -d $DB_NAME <<EOF
GRANT ALL PRIVILEGES ON DATABASE $DB_NAME TO $DB_USER;
GRANT ALL ON SCHEMA public TO $DB_USER;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO $DB_USER;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO $DB_USER;
EOF

echo -e "${GREEN}✓${NC} Database and user created/verified"

# Test connection
echo ""
echo -e "${BLUE}Testing connection...${NC}"
if PGPASSWORD=$DB_PASSWORD psql -h localhost -U $DB_USER -d $DB_NAME -c "SELECT 1;" > /dev/null 2>&1; then
    echo -e "${GREEN}✓${NC} Connection successful!"
else
    echo -e "${RED}✗${NC} Connection failed"
    exit 1
fi

echo ""
echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}PostgreSQL Setup Complete!${NC}"
echo -e "${GREEN}============================================${NC}"
echo ""
echo -e "${BLUE}Database Details:${NC}"
echo -e "  Host:     ${YELLOW}localhost${NC}"
echo -e "  Port:     ${YELLOW}5432${NC}"
echo -e "  Database: ${YELLOW}$DB_NAME${NC}"
echo -e "  User:     ${YELLOW}$DB_USER${NC}"
echo -e "  Password: ${YELLOW}$DB_PASSWORD${NC}"
echo ""
echo -e "${BLUE}Connect with:${NC}"
echo -e "  ${YELLOW}psql -h localhost -U $DB_USER -d $DB_NAME${NC}"
echo ""
echo -e "${BLUE}Connection string:${NC}"
echo -e "  ${YELLOW}jdbc:postgresql://localhost:5432/$DB_NAME${NC}"
echo ""
