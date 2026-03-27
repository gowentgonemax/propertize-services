# User Authentication API Testing Guide

## Overview
Your Spring Boot application now has a complete user authentication system that authenticates users from the MySQL database.

## What Was Fixed
1. ✅ Fixed circular dependency errors using `@Lazy` annotation
2. ✅ Replaced all `javax.validation` imports with `jakarta.validation` (Spring Boot 3+ requirement)
3. ✅ Removed conflicting validation dependencies from pom.xml
4. ✅ Added JWT configuration to application-dev.yml
5. ✅ Created registration and login endpoints
6. ✅ Project builds successfully

## Available Endpoints

### 1. Register a New User
**Endpoint:** `POST http://localhost:8080/auth/register`

**Request Body:**
```json
{
  "email": "john.doe@example.com",
  "password": "password123",
  "firstName": "John",
  "lastName": "Doe"
}
```

**Response (201 Created):**
```json
{
  "message": "User registered successfully",
  "email": "john.doe@example.com"
}
```

### 2. Login with User from Database
**Endpoint:** `POST http://localhost:8080/auth/login`

**Request Body:**
```json
{
  "email": "john.doe@example.com",
  "password": "password123"
}
```

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "refresh-eyJhbGciOiJIUzI1NiJ9...",
  "expiresIn": 3600
}
```

## How to Test

### Step 1: Start the Application
```bash
cd /Users/ravishah/Downloads/wagecraft
export JAVA_HOME=/Users/ravishah/Library/Java/JavaVirtualMachines/corretto-24.0.2/Contents/Home
mvn spring-boot:run
```

### Step 2: Test with cURL

**Register a user:**
```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123",
    "firstName": "Test",
    "lastName": "User"
  }'
```

**Login with the user:**
```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }'
```

**Access a protected endpoint:**
```bash
curl -X GET http://localhost:8080/api/employees \
  -H "Authorization: Bearer YOUR_JWT_TOKEN_HERE"
```

### Step 3: Test with Postman or any REST Client

1. **Register:** POST to `http://localhost:8080/auth/register` with the JSON body
2. **Login:** POST to `http://localhost:8080/auth/login` with email and password
3. **Use Token:** Copy the `token` from the login response
4. **Access Protected Routes:** Add header `Authorization: Bearer YOUR_TOKEN`

## How It Works

### Authentication Flow:
1. **User Registration:**
   - Password is automatically hashed using BCrypt
   - User data is stored in the MySQL database (`users` table)

2. **User Login:**
   - System validates credentials against database
   - If valid, generates a JWT token (expires in 1 hour)
   - Token is signed with the secret key from application-dev.yml

3. **Protected Endpoints:**
   - All requests to `/api/*` require a valid JWT token
   - Token is verified using `JwtRequestFilter`
   - User details are loaded from the database

## Database Schema

The `users` table structure:
```sql
CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

## Security Configuration

- **JWT Secret:** Configured in `application-dev.yml`
- **Token Expiration:** 3600 seconds (1 hour)
- **Password Encoding:** BCrypt
- **Session Management:** Stateless (JWT-based)

## Troubleshooting

### Issue: "Bad credentials" error
- Ensure the email exists in the database
- Verify the password is correct
- Check that the user's `enabled` field is `true`

### Issue: "User not found"
- The email doesn't exist in the database
- Register the user first using `/auth/register`

### Issue: Token expired
- Login again to get a new token
- Tokens expire after 1 hour

## Next Steps

You can now:
- ✅ Register users via API
- ✅ Login users from the database
- ✅ Access protected endpoints with JWT token
- ✅ User passwords are securely hashed in the database

Consider adding:
- Refresh token implementation
- Email verification
- Password reset functionality
- Role-based access control (RBAC)

