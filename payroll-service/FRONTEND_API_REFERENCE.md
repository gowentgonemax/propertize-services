# WageCraft API Reference for Frontend Development

## Base URL
- **Development**: `http://localhost:8080`
- **QA**: Configure based on your QA environment
- **Production**: Configure based on your production environment

## Authentication
All API endpoints (except `/auth/register` and `/auth/login`) require JWT authentication.

**Header Format:**
```
Authorization: Bearer <your_jwt_token>
```

---

## 1. Authentication APIs

### 1.1 Register User
**Endpoint:** `POST /auth/register`

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "password123",
  "firstName": "John",
  "lastName": "Doe"
}
```

**Response:** `201 Created`
```json
{
  "id": "uuid",
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "message": "User registered successfully"
}
```

**Error Response:** `400 Bad Request`
```json
{
  "error": "Validation failed",
  "message": "Email already exists"
}
```

### 1.2 Login
**Endpoint:** `POST /auth/login`

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response:** `200 OK`
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 86400
}
```

**Error Response:** `401 Unauthorized`
```json
{
  "error": "Authentication failed",
  "message": "Invalid credentials"
}
```

---

## 2. Client APIs

### 2.1 Get All Clients
**Endpoint:** `GET /clients`

**Query Parameters:**
- `page` (optional, default: 0): Page number
- `limit` (optional, default: 20): Items per page
- `status` (optional): Filter by status (`ACTIVE`, `INACTIVE`, `SUSPENDED`)

**Example:**
```
GET /clients?page=0&limit=20&status=ACTIVE
```

**Response:** `200 OK`
```json
{
  "content": [
    {
      "id": "uuid",
      "companyName": "Acme Corporation",
      "taxId": "12-3456789",
      "address": {
        "street": "123 Main St",
        "city": "New York",
        "state": "NY",
        "zipCode": "10001",
        "country": "USA"
      },
      "contactPerson": "Jane Smith",
      "email": "contact@acme.com",
      "phone": "555-1234",
      "status": "ACTIVE",
      "payrollSchedule": "BIWEEKLY",
      "createdAt": "2025-01-15T10:00:00",
      "updatedAt": "2025-01-15T10:00:00"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 50,
  "totalPages": 3,
  "last": false
}
```

### 2.2 Get Client by ID
**Endpoint:** `GET /clients/{id}`

**Response:** `200 OK`
```json
{
  "id": "uuid",
  "companyName": "Acme Corporation",
  "taxId": "12-3456789",
  "address": {
    "street": "123 Main St",
    "city": "New York",
    "state": "NY",
    "zipCode": "10001",
    "country": "USA"
  },
  "contactPerson": "Jane Smith",
  "email": "contact@acme.com",
  "phone": "555-1234",
  "status": "ACTIVE",
  "payrollSchedule": "BIWEEKLY",
  "createdAt": "2025-01-15T10:00:00",
  "updatedAt": "2025-01-15T10:00:00"
}
```

### 2.3 Create Client
**Endpoint:** `POST /clients`

**Request Body:**
```json
{
  "companyName": "Acme Corporation",
  "taxId": "12-3456789",
  "address": {
    "street": "123 Main St",
    "city": "New York",
    "state": "NY",
    "zipCode": "10001",
    "country": "USA"
  },
  "contactPerson": "Jane Smith",
  "email": "contact@acme.com",
  "phone": "555-1234",
  "status": "ACTIVE",
  "payrollSchedule": "BIWEEKLY"
}
```

**Response:** `201 Created`
```json
{
  "id": "uuid",
  "companyName": "Acme Corporation",
  ...
}
```

### 2.4 Update Client
**Endpoint:** `PUT /clients/{id}`

**Request Body:** (Same as Create Client)

**Response:** `200 OK`

### 2.5 Delete Client
**Endpoint:** `DELETE /clients/{id}`

**Response:** `204 No Content`

---

## 3. Employee APIs

### 3.1 Get Employees by Client
**Endpoint:** `GET /clients/{clientId}/employees`

**Query Parameters:**
- `page` (optional, default: 0): Page number
- `limit` (optional, default: 20): Items per page
- `status` (optional): Filter by status (`ACTIVE`, `INACTIVE`, `TERMINATED`, `ON_LEAVE`)
- `department` (optional): Filter by department

**Example:**
```
GET /clients/uuid/employees?page=0&limit=20&status=ACTIVE&department=Engineering
```

**Response:** `200 OK`
```json
{
  "content": [
    {
      "id": "uuid",
      "employeeNumber": "EMP001",
      "firstName": "John",
      "lastName": "Doe",
      "email": "john.doe@acme.com",
      "phone": "555-5678",
      "address": {
        "street": "456 Oak Ave",
        "city": "New York",
        "state": "NY",
        "zipCode": "10002",
        "country": "USA"
      },
      "ssn": "***-**-1234",
      "dateOfBirth": "1990-05-15",
      "hireDate": "2023-01-10",
      "terminationDate": null,
      "department": "Engineering",
      "position": "Software Engineer",
      "employmentType": "FULL_TIME",
      "payType": "SALARY",
      "payRate": 85000.00,
      "status": "ACTIVE",
      "createdAt": "2023-01-10T09:00:00",
      "updatedAt": "2023-01-10T09:00:00"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 150,
  "totalPages": 8,
  "last": false
}
```

### 3.2 Get Employee by ID
**Endpoint:** `GET /employees/{id}`

**Response:** `200 OK`
```json
{
  "id": "uuid",
  "employeeNumber": "EMP001",
  "firstName": "John",
  "lastName": "Doe",
  ...
}
```

### 3.3 Create Employee
**Endpoint:** `POST /clients/{clientId}/employees`

**Request Body:**
```json
{
  "employeeNumber": "EMP001",
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@acme.com",
  "phone": "555-5678",
  "address": {
    "street": "456 Oak Ave",
    "city": "New York",
    "state": "NY",
    "zipCode": "10002",
    "country": "USA"
  },
  "ssn": "123-45-6789",
  "dateOfBirth": "1990-05-15",
  "hireDate": "2023-01-10",
  "department": "Engineering",
  "position": "Software Engineer",
  "employmentType": "FULL_TIME",
  "payType": "SALARY",
  "payRate": 85000.00,
  "status": "ACTIVE"
}
```

**Response:** `201 Created`

### 3.4 Update Employee
**Endpoint:** `PUT /employees/{id}`

**Request Body:** (Same as Create Employee)

**Response:** `200 OK`

### 3.5 Terminate Employee
**Endpoint:** `POST /employees/{id}/terminate`

**Response:** `204 No Content`

---

## 4. Payroll APIs

### 4.1 Get Payroll Runs
**Endpoint:** `GET /clients/{clientId}/payroll`

**Query Parameters:**
- `page` (optional, default: 0): Page number
- `limit` (optional, default: 20): Items per page
- `startDate` (optional): Filter by date range start (YYYY-MM-DD)
- `endDate` (optional): Filter by date range end (YYYY-MM-DD)
- `status` (optional): Filter by status (`DRAFT`, `PROCESSING`, `PROCESSED`, `APPROVED`, `PAID`, `CANCELLED`)

**Example:**
```
GET /clients/uuid/payroll?page=0&limit=20&startDate=2025-01-01&endDate=2025-01-31
```

**Response:** `200 OK`
```json
{
  "content": [
    {
      "id": "uuid",
      "payPeriodStart": "2025-01-01",
      "payPeriodEnd": "2025-01-15",
      "payDate": "2025-01-20",
      "status": "APPROVED",
      "totalGrossPay": 250000.00,
      "totalNetPay": 185000.00,
      "totalTaxes": 50000.00,
      "totalDeductions": 15000.00,
      "employeeCount": 150,
      "processedAt": "2025-01-18T14:30:00",
      "approvedAt": "2025-01-19T10:00:00",
      "approvedBy": {
        "id": "uuid",
        "email": "manager@acme.com",
        "firstName": "Jane",
        "lastName": "Manager"
      },
      "createdAt": "2025-01-16T09:00:00",
      "updatedAt": "2025-01-19T10:00:00"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 24,
  "totalPages": 2,
  "last": false
}
```

### 4.2 Get Payroll Run by ID
**Endpoint:** `GET /clients/{clientId}/payroll/{payrollId}`

**Response:** `200 OK`
```json
{
  "id": "uuid",
  "payPeriodStart": "2025-01-01",
  "payPeriodEnd": "2025-01-15",
  "payDate": "2025-01-20",
  "status": "APPROVED",
  "totalGrossPay": 250000.00,
  "totalNetPay": 185000.00,
  "totalTaxes": 50000.00,
  "totalDeductions": 15000.00,
  "employeeCount": 150,
  "processedAt": "2025-01-18T14:30:00",
  "approvedAt": "2025-01-19T10:00:00",
  "createdAt": "2025-01-16T09:00:00",
  "updatedAt": "2025-01-19T10:00:00"
}
```

### 4.3 Create Payroll Run
**Endpoint:** `POST /clients/{clientId}/payroll`

**Request Body:**
```json
{
  "payPeriodStart": "2025-01-01",
  "payPeriodEnd": "2025-01-15",
  "payDate": "2025-01-20",
  "status": "DRAFT"
}
```

**Response:** `201 Created`

### 4.4 Process Payroll Run
**Endpoint:** `POST /clients/{clientId}/payroll/{payrollId}/process`

**Description:** Calculates all employee paychecks, taxes, and deductions

**Response:** `200 OK`
```json
{
  "id": "uuid",
  "status": "PROCESSED",
  "totalGrossPay": 250000.00,
  "totalNetPay": 185000.00,
  "totalTaxes": 50000.00,
  "totalDeductions": 15000.00,
  "employeeCount": 150,
  "processedAt": "2025-01-18T14:30:00"
}
```

### 4.5 Approve Payroll Run
**Endpoint:** `POST /clients/{clientId}/payroll/{payrollId}/approve`

**Description:** Approves the processed payroll for payment

**Response:** `200 OK`
```json
{
  "id": "uuid",
  "status": "APPROVED",
  "approvedAt": "2025-01-19T10:00:00",
  "approvedBy": {
    "id": "uuid",
    "email": "manager@acme.com"
  }
}
```

---

## Common Error Responses

### 400 Bad Request
```json
{
  "error": "Validation failed",
  "message": "Invalid input data"
}
```

### 401 Unauthorized
```json
{
  "error": "Authentication failed",
  "message": "Invalid or expired token"
}
```

### 403 Forbidden
```json
{
  "error": "Access denied",
  "message": "You don't have permission to access this resource"
}
```

### 404 Not Found
```json
{
  "error": "Resource not found",
  "message": "The requested resource does not exist"
}
```

### 500 Internal Server Error
```json
{
  "error": "Internal server error",
  "message": "An unexpected error occurred"
}
```

---

## Response Headers
All responses include:
- `X-Correlation-ID`: Unique request identifier for debugging
- `Content-Type`: `application/json`

## Pagination
All list endpoints return paginated results with the following structure:
```json
{
  "content": [],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20
  },
  "totalElements": 100,
  "totalPages": 5,
  "last": false,
  "first": true
}
```

