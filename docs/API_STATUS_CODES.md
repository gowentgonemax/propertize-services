# Propertize API — Endpoint & Status Code Reference

> Auto-generated reference of all REST API endpoints and their HTTP response status codes across all services.

---

## Table of Contents

- [Global Status Codes & Exception Handling](#global-status-codes--exception-handling)
- [Auth Service (8081)](#1-auth-service-port-8081)
- [Propertize Service (8082)](#2-propertize-service-port-8082)
- [Employee Service (8083)](#3-employee-service-port-8083)
- [Payment Service (8084)](#4-payment-service-port-8084)
- [Payroll Service (8085)](#5-payroll-service-port-8085)
- [API Gateway (8080)](#6-api-gateway-port-8080)
- [Python Services (8090-8093)](#7-python-services)
- [Service Registry (8761)](#8-service-registry-port-8761)

---

## Global Status Codes & Exception Handling

### Standard HTTP Status Codes

| Code | Meaning                | When Returned                                                      |
| ---- | ---------------------- | ------------------------------------------------------------------ |
| 200  | OK                     | Successful GET, PUT, PATCH, or action POST                         |
| 201  | Created                | Successful POST creating a new resource                            |
| 204  | No Content             | Successful DELETE                                                  |
| 207  | Multi-Status           | Partial success in bulk operations                                 |
| 400  | Bad Request            | Validation errors, missing fields, malformed JSON                  |
| 401  | Unauthorized           | Missing/invalid JWT, expired credentials                           |
| 403  | Forbidden              | RBAC permission denied, read-only role attempting write            |
| 404  | Not Found              | Resource does not exist                                            |
| 405  | Method Not Allowed     | Wrong HTTP method for endpoint                                     |
| 409  | Conflict               | Invalid state transition, duplicate resource, constraint violation |
| 415  | Unsupported Media Type | Invalid Content-Type header                                        |
| 422  | Unprocessable Entity   | Business logic validation failure                                  |
| 429  | Too Many Requests      | Rate limit exceeded (login endpoint)                               |
| 500  | Internal Server Error  | Unhandled exception                                                |
| 502  | Bad Gateway            | Upstream service failure                                           |
| 503  | Service Unavailable    | Dependency down (DB, MinIO, Stripe, etc.)                          |
| 504  | Gateway Timeout        | Upstream service timeout                                           |

### Exception Handler Chain

**PropertizeGlobalExceptionHandler** (propertize-commons — base for all Java services):

| Exception                         | Status | Code Constant              |
| --------------------------------- | ------ | -------------------------- |
| `ResourceNotFoundException`       | 404    | `RESOURCE_NOT_FOUND`       |
| `InvalidStateTransitionException` | 409    | `INVALID_STATE_TRANSITION` |
| `UpstreamServiceException`        | 502    | `UPSTREAM_SERVICE_ERROR`   |
| `MethodArgumentNotValidException` | 400    | `VALIDATION_FAILED`        |
| `DataIntegrityViolationException` | 409    | `CONFLICT`                 |
| `Exception` (catch-all)           | 500    | `INTERNAL_ERROR`           |

**Propertize Service** extends base with:

| Exception                                | Status |
| ---------------------------------------- | ------ |
| `ValidationException`                    | 400    |
| `BusinessLogicException`                 | 422    |
| `ResourceAlreadyExistsException`         | 409    |
| `HttpRequestMethodNotSupportedException` | 405    |
| `HttpMediaTypeNotSupportedException`     | 415    |

### Authentication Headers

All protected endpoints require:

| Header                        | Source                   | Required |
| ----------------------------- | ------------------------ | -------- |
| `Authorization: Bearer <JWT>` | Client                   | Yes      |
| `X-User-ID`                   | Gateway (injected)       | Auto     |
| `X-User-Email`                | Gateway (injected)       | Auto     |
| `X-User-Roles`                | Gateway (injected)       | Auto     |
| `X-Correlation-ID`            | Client or auto-generated | Optional |

---

## 1. Auth Service (Port 8081)

### AuthController — `/api/v1/auth`

| Method | Endpoint                | Status Codes  | Description                     |
| ------ | ----------------------- | ------------- | ------------------------------- |
| POST   | `/login`                | 200, 401, 429 | User login with rate limiting   |
| POST   | `/refresh`              | 200, 401      | Refresh access token            |
| POST   | `/logout`               | 200           | Logout & blacklist token        |
| POST   | `/forgot-password`      | 200, 400      | Initiate password reset         |
| POST   | `/reset-password`       | 200, 400, 401 | Complete password reset         |
| GET    | `/validate-reset-token` | 200, 400, 401 | Validate reset token            |
| POST   | `/change-password`      | 200, 400, 401 | Change password (authenticated) |
| GET    | `/me`                   | 200, 401      | Get current user profile        |
| PUT    | `/me`                   | 200, 400, 401 | Update current user profile     |
| POST   | `/switch-organization`  | 200, 400, 401 | Switch active organization      |

### RbacController — `/api/v1/rbac`

| Method | Endpoint                                       | Status Codes  | Description                      |
| ------ | ---------------------------------------------- | ------------- | -------------------------------- |
| POST   | `/authorize`                                   | 200, 400, 401 | Check authorization              |
| GET    | `/permissions/{role}`                          | 200, 404      | Get permissions for role         |
| POST   | `/permissions/resolve`                         | 200, 400      | Resolve hierarchical permissions |
| POST   | `/permissions/check`                           | 200, 400      | Check specific permission        |
| GET    | `/roles`                                       | 200           | List all roles                   |
| GET    | `/roles/{role}`                                | 200, 404      | Get role details                 |
| GET    | `/roles/scope/{scope}`                         | 200           | Get roles by scope               |
| GET    | `/rbac/config`                                 | 200           | Get RBAC configuration           |
| GET    | `/rbac/endpoints`                              | 200           | List protected endpoints         |
| POST   | `/cache/invalidate`                            | 200, 401      | Invalidate RBAC cache            |
| POST   | `/temporal-permissions`                        | 201, 400      | Grant time-limited permission    |
| GET    | `/temporal-permissions/user/{userId}`          | 200           | Get user's temporal permissions  |
| DELETE | `/temporal-permissions/{id}`                   | 204, 404      | Revoke temporal permission       |
| GET    | `/temporal-permissions/granted-by/{grantorId}` | 200           | Permissions by grantor           |
| POST   | `/composite-roles`                             | 201, 400      | Create composite role            |
| GET    | `/composite-roles`                             | 200           | List composite roles             |
| GET    | `/composite-roles/{id}`                        | 200, 404      | Get composite role               |
| DELETE | `/composite-roles/{id}`                        | 204, 404      | Delete composite role            |
| POST   | `/resolve-permissions`                         | 200, 400      | Resolve final permission set     |
| POST   | `/delegations`                                 | 201, 400      | Create delegation                |
| GET    | `/delegations/by-me`                           | 200           | Delegations I created            |
| GET    | `/delegations/to-me`                           | 200           | Delegations assigned to me       |
| DELETE | `/delegations/{id}`                            | 204, 404      | Revoke delegation                |
| GET    | `/delegation-rules`                            | 200           | List delegation rules            |
| POST   | `/custom-roles`                                | 201, 400      | Create custom role               |
| GET    | `/custom-roles`                                | 200           | List custom roles                |
| GET    | `/custom-roles/{id}`                           | 200, 404      | Get custom role                  |
| PUT    | `/custom-roles/{id}`                           | 200, 400, 404 | Update custom role               |
| DELETE | `/custom-roles/{id}`                           | 204, 404      | Delete custom role               |
| GET    | `/rbac/audit-logs`                             | 200           | Get RBAC audit logs              |
| GET    | `/rbac/audit-logs/denials`                     | 200           | Get permission denial logs       |
| GET    | `/rbac/audit-logs/summary`                     | 200           | Audit log summary                |
| POST   | `/rbac/ip-rules`                               | 201, 400      | Create IP restriction rule       |
| GET    | `/rbac/ip-rules`                               | 200           | List IP rules                    |
| DELETE | `/rbac/ip-rules/{id}`                          | 204, 404      | Delete IP rule                   |
| POST   | `/rbac/ip-rules/check`                         | 200, 401      | Check IP restriction             |
| GET    | `/fields/{resource}/{role}`                    | 200           | Get field-level visibility       |
| POST   | `/fields/resolve`                              | 200, 400      | Resolve accessible fields        |
| GET    | `/fields/resources`                            | 200           | List protected resources         |
| POST   | `/scope/resolve`                               | 200, 400      | Resolve data scope               |
| GET    | `/conditions/{role}/{permission}`              | 200           | Get permission conditions        |
| POST   | `/time/check`                                  | 200, 401      | Check time-based access          |

### RbacPublicController — `/api/v1/rbac/public`

| Method | Endpoint       | Status Codes | Description                   |
| ------ | -------------- | ------------ | ----------------------------- |
| GET    | `/roles`       | 200          | List all roles (public)       |
| GET    | `/permissions` | 200          | List all permissions (public) |

### UserManagementController — `/api/v1/users`

| Method | Endpoint               | Status Codes  | Description          |
| ------ | ---------------------- | ------------- | -------------------- |
| GET    | `/`                    | 200           | List users           |
| POST   | `/`                    | 201, 400      | Create user          |
| GET    | `/{id}`                | 200, 404      | Get user by ID       |
| GET    | `/username/{username}` | 200, 404      | Get user by username |
| GET    | `/email/{email}`       | 200, 404      | Get user by email    |
| PUT    | `/{id}`                | 200, 400, 404 | Update user          |
| PUT    | `/{id}/password`       | 200, 400, 404 | Update user password |

### CustomRoleController — `/api/v1/custom-roles`

| Method | Endpoint                       | Status Codes  | Description             |
| ------ | ------------------------------ | ------------- | ----------------------- |
| POST   | `/`                            | 201, 400      | Create custom role      |
| GET    | `/`                            | 200           | List custom roles       |
| GET    | `/{id}`                        | 200, 404      | Get custom role         |
| PUT    | `/{id}`                        | 200, 400, 404 | Update custom role      |
| DELETE | `/{id}`                        | 204, 404      | Delete custom role      |
| POST   | `/{id}/assign`                 | 200, 400, 404 | Assign role to user     |
| DELETE | `/{id}/assign/{userId}`        | 204, 404      | Unassign role from user |
| GET    | `/users/{userId}/custom-roles` | 200           | Get user's custom roles |

### HealthCheckController — `/`

| Method | Endpoint | Status Codes | Description        |
| ------ | -------- | ------------ | ------------------ |
| GET    | `/`      | 200          | Basic health check |

---

## 2. Propertize Service (Port 8082)

### PropertyController — `/api/v1/properties`

| Method | Endpoint                                   | Status Codes       | Description                 |
| ------ | ------------------------------------------ | ------------------ | --------------------------- |
| GET    | `/`                                        | 200                | List properties (paginated) |
| GET    | `/{id}`                                    | 200, 400, 404      | Get property by ID          |
| GET    | `/statistics`                              | 200                | Property statistics         |
| POST   | `/`                                        | 201, 400           | Create property             |
| POST   | `/bulk`                                    | 207, 400           | Bulk create properties      |
| PUT    | `/{id}`                                    | 200, 400, 404, 409 | Update property             |
| PATCH  | `/{id}`                                    | 200, 400, 404      | Partial update              |
| DELETE | `/{id}`                                    | 204, 404           | Delete property             |
| GET    | `/public`                                  | 200                | List public properties      |
| GET    | `/public/available`                        | 200                | List available properties   |
| GET    | `/public/listing/{listingCode}`            | 200, 404           | Get by listing code         |
| GET    | `/organization/{organizationId}`           | 200                | List org properties         |
| GET    | `/by-organization-code/{organizationCode}` | 200                | List by org code            |

### TenantController — `/api/v1/tenants`

| Method | Endpoint                | Status Codes       | Description            |
| ------ | ----------------------- | ------------------ | ---------------------- |
| GET    | `/`                     | 200                | List tenants           |
| GET    | `/me`                   | 200, 401           | Get current tenant     |
| GET    | `/{id}`                 | 200, 404           | Get tenant             |
| POST   | `/`                     | 201, 400           | Create tenant          |
| PUT    | `/{id}`                 | 200, 400, 404, 409 | Update tenant          |
| PATCH  | `/{id}`                 | 200, 400, 404      | Partial update         |
| DELETE | `/{id}`                 | 204, 404           | Delete tenant          |
| GET    | `/{id}/payment-history` | 200, 404           | Tenant payment history |
| GET    | `/statistics`           | 200                | Tenant statistics      |

### MaintenanceRequestController — `/api/v1/maintenance-requests`

| Method | Endpoint                                       | Status Codes            | Description                |
| ------ | ---------------------------------------------- | ----------------------- | -------------------------- |
| GET    | `/`                                            | 200                     | List requests (paginated)  |
| GET    | `/{id}`                                        | 200, 404                | Get request                |
| GET    | `/open`                                        | 200                     | Get open requests          |
| GET    | `/assigned/{assignedTo}`                       | 200                     | Assigned requests          |
| GET    | `/count/status/{status}`                       | 200                     | Count by status            |
| GET    | `/count/property/{propertyId}/status/{status}` | 200                     | Count by property & status |
| POST   | `/`                                            | 201, 400                | Create request             |
| PUT    | `/{id}`                                        | 200, 400, 404, 409      | Update request             |
| DELETE | `/{id}`                                        | 204, 404                | Delete request             |
| POST   | `/{id}/notes`                                  | 201, 400                | Add note                   |
| GET    | `/{id}/notes`                                  | 200, 404                | Get notes                  |
| GET    | `/notes/recent`                                | 200                     | Get recent notes           |
| POST   | `/tenant/submit`                               | 201, 400                | Tenant submit request      |
| GET    | `/tenant/my-requests`                          | 200, 401                | My requests                |
| GET    | `/tenant/request/{id}`                         | 200, 401, 404           | Get my specific request    |
| PUT    | `/tenant/request/{id}/cancel`                  | 200, 400, 401, 404, 409 | Cancel my request          |
| GET    | `/tenant/count/open`                           | 200, 401                | Count my open requests     |
| GET    | `/tenant/summary`                              | 200, 401                | My requests summary        |
| GET    | `/statistics`                                  | 200                     | Maintenance statistics     |

### LeaseController — `/api/v1/leases`

| Method | Endpoint | Status Codes       | Description  |
| ------ | -------- | ------------------ | ------------ |
| GET    | `/`      | 200                | List leases  |
| GET    | `/{id}`  | 200, 404           | Get lease    |
| POST   | `/`      | 201, 400           | Create lease |
| PUT    | `/{id}`  | 200, 400, 404, 409 | Update lease |
| DELETE | `/{id}`  | 204, 404           | Delete lease |

### InvoiceController — `/api/v1/invoices`

| Method | Endpoint | Status Codes  | Description    |
| ------ | -------- | ------------- | -------------- |
| GET    | `/`      | 200           | List invoices  |
| GET    | `/{id}`  | 200, 404      | Get invoice    |
| POST   | `/`      | 201, 400      | Create invoice |
| PUT    | `/{id}`  | 200, 400, 404 | Update invoice |
| DELETE | `/{id}`  | 204, 404      | Delete invoice |

### DocumentController — `/api/v1/documents`

| Method | Endpoint  | Status Codes  | Description     |
| ------ | --------- | ------------- | --------------- |
| POST   | `/upload` | 201, 400, 500 | Upload document |
| GET    | `/`       | 200           | List documents  |
| GET    | `/{id}`   | 200, 404      | Get document    |
| DELETE | `/{id}`   | 204, 404      | Delete document |

### NotificationController — `/api/v1/notifications`

| Method | Endpoint             | Status Codes | Description        |
| ------ | -------------------- | ------------ | ------------------ |
| GET    | `/`                  | 200          | List notifications |
| GET    | `/{id}`              | 200, 404     | Get notification   |
| PUT    | `/{id}/mark-as-read` | 200, 404     | Mark as read       |

### NotificationPreferenceController — `/api/v1/notification-preferences`

| Method | Endpoint                 | Status Codes  | Description            |
| ------ | ------------------------ | ------------- | ---------------------- |
| GET    | `/tenant/{tenantId}`     | 200, 404      | Get preferences        |
| POST   | `/grant-consent`         | 201, 400      | Grant consent          |
| POST   | `/revoke-consent`        | 200, 400, 404 | Revoke consent         |
| POST   | `/opt-out`               | 200, 400      | Opt out from channel   |
| PUT    | `/update`                | 200, 400      | Update preference      |
| POST   | `/initialize/{tenantId}` | 201, 404      | Initialize preferences |
| GET    | `/check-consent`         | 200           | Check consent status   |

### SMSNotificationController — `/api/v1/sms-notifications`

| Method | Endpoint              | Status Codes  | Description            |
| ------ | --------------------- | ------------- | ---------------------- |
| POST   | `/send`               | 200, 400, 503 | Send SMS               |
| POST   | `/payment-reminder`   | 200, 400, 503 | Payment reminder SMS   |
| POST   | `/application-status` | 200, 400, 503 | Application status SMS |
| POST   | `/maintenance-update` | 200, 400, 503 | Maintenance update SMS |

### SearchController — `/api/v1/search`

| Method | Endpoint  | Status Codes | Description           |
| ------ | --------- | ------------ | --------------------- |
| GET    | `/`       | 200, 400     | Global search         |
| POST   | `/rerank` | 200, 400     | Rerank search results |

### AnalyticsController — `/api/v1/analytics`

| Method | Endpoint    | Status Codes | Description       |
| ------ | ----------- | ------------ | ----------------- |
| POST   | `/events`   | 200, 400     | Track event       |
| POST   | `/track`    | 200, 400     | Track analytics   |
| POST   | `/pageview` | 200, 400     | Track page view   |
| GET    | `/summary`  | 200          | Analytics summary |
| GET    | `/events`   | 200          | Get events        |

### VendorController — `/api/v1/vendors`

| Method | Endpoint | Status Codes       | Description   |
| ------ | -------- | ------------------ | ------------- |
| GET    | `/`      | 200                | List vendors  |
| GET    | `/{id}`  | 200, 404           | Get vendor    |
| POST   | `/`      | 201, 400           | Create vendor |
| PUT    | `/{id}`  | 200, 400, 404, 409 | Update vendor |
| DELETE | `/{id}`  | 204, 404           | Delete vendor |

### UtilityController — `/api/v1/utilities`

| Method | Endpoint                       | Status Codes  | Description           |
| ------ | ------------------------------ | ------------- | --------------------- |
| GET    | `/`                            | 200           | List utilities        |
| GET    | `/{id}`                        | 200, 404      | Get utility           |
| GET    | `/property/{propertyId}`       | 200, 404      | Utilities by property |
| GET    | `/tenant/{tenantId}`           | 200, 404      | Utilities by tenant   |
| GET    | `/type/{type}`                 | 200           | Utilities by type     |
| GET    | `/property/{propertyId}/count` | 200, 404      | Count utilities       |
| POST   | `/`                            | 201, 400      | Create utility        |
| PUT    | `/{id}`                        | 200, 400, 404 | Update utility        |
| DELETE | `/{id}`                        | 204, 404      | Delete utility        |

### OrganizationController — `/api/v1/organizations`

| Method | Endpoint | Status Codes       | Description         |
| ------ | -------- | ------------------ | ------------------- |
| GET    | `/`      | 200                | List organizations  |
| GET    | `/{id}`  | 200, 404           | Get organization    |
| POST   | `/`      | 201, 400           | Create organization |
| PUT    | `/{id}`  | 200, 400, 404, 409 | Update organization |
| PATCH  | `/{id}`  | 200, 400, 404      | Partial update      |
| DELETE | `/{id}`  | 204, 405, 404      | Delete organization |

### ApprovalWorkflowController — `/api/v1/approvals`

| Method | Endpoint | Status Codes       | Description     |
| ------ | -------- | ------------------ | --------------- |
| GET    | `/`      | 200                | List approvals  |
| POST   | `/`      | 201, 400           | Create approval |
| PUT    | `/{id}`  | 200, 400, 404, 409 | Update approval |

### RentalApplicationController — `/api/v1/rental-applications`

| Method | Endpoint | Status Codes       | Description        |
| ------ | -------- | ------------------ | ------------------ |
| GET    | `/`      | 200                | List applications  |
| GET    | `/{id}`  | 200, 404           | Get application    |
| POST   | `/`      | 201, 400           | Create application |
| PUT    | `/{id}`  | 200, 400, 404, 409 | Update application |
| DELETE | `/{id}`  | 204, 404           | Delete application |

### TenantScreeningController — `/api/v1/screening`

| Method | Endpoint | Status Codes  | Description      |
| ------ | -------- | ------------- | ---------------- |
| GET    | `/`      | 200           | List screenings  |
| POST   | `/`      | 201, 400, 404 | Create screening |

### LateFeeController — `/api/v1/late-fees`

| Method | Endpoint | Status Codes       | Description     |
| ------ | -------- | ------------------ | --------------- |
| GET    | `/`      | 200                | List late fees  |
| GET    | `/{id}`  | 200, 404           | Get late fee    |
| POST   | `/`      | 201, 400           | Create late fee |
| PUT    | `/{id}`  | 200, 400, 404, 409 | Update late fee |
| DELETE | `/{id}`  | 204, 404           | Delete late fee |

### TaskController — `/api/v1/tasks`

| Method | Endpoint | Status Codes  | Description |
| ------ | -------- | ------------- | ----------- |
| GET    | `/`      | 200           | List tasks  |
| GET    | `/{id}`  | 200, 404      | Get task    |
| POST   | `/`      | 201, 400      | Create task |
| PUT    | `/{id}`  | 200, 400, 404 | Update task |
| DELETE | `/{id}`  | 204, 404      | Delete task |

### MilestoneController — `/api/v1/milestones`

| Method | Endpoint | Status Codes  | Description      |
| ------ | -------- | ------------- | ---------------- |
| GET    | `/`      | 200           | List milestones  |
| GET    | `/{id}`  | 200, 404      | Get milestone    |
| POST   | `/`      | 201, 400      | Create milestone |
| PUT    | `/{id}`  | 200, 400, 404 | Update milestone |
| DELETE | `/{id}`  | 204, 404      | Delete milestone |

### ExpenseController — `/api/v1/expenses`

| Method | Endpoint | Status Codes  | Description    |
| ------ | -------- | ------------- | -------------- |
| GET    | `/`      | 200           | List expenses  |
| GET    | `/{id}`  | 200, 404      | Get expense    |
| POST   | `/`      | 201, 400      | Create expense |
| PUT    | `/{id}`  | 200, 400, 404 | Update expense |
| DELETE | `/{id}`  | 204, 404      | Delete expense |

### AdminOrganizationManagementController — `/api/v1/admin/organizations`

| Method | Endpoint | Status Codes       | Description                    |
| ------ | -------- | ------------------ | ------------------------------ |
| GET    | `/`      | 200                | List all organizations (admin) |
| POST   | `/`      | 201, 400           | Create organization (admin)    |
| PUT    | `/{id}`  | 200, 400, 404, 409 | Update organization (admin)    |

### AdminStatsController — `/api/v1/admin/stats`

| Method | Endpoint | Status Codes | Description              |
| ------ | -------- | ------------ | ------------------------ |
| GET    | `/`      | 200          | Platform-wide statistics |

### AdminContactController — `/api/v1/admin/contact`

| Method | Endpoint | Status Codes  | Description    |
| ------ | -------- | ------------- | -------------- |
| GET    | `/`      | 200           | List contacts  |
| PUT    | `/{id}`  | 200, 400, 404 | Update contact |
| DELETE | `/{id}`  | 204, 404      | Delete contact |

### Additional Controllers

| Method | Endpoint               | Status Codes  | Description              |
| ------ | ---------------------- | ------------- | ------------------------ |
| GET    | `/api/v1/reports/**`   | 200, 400, 500 | Report generation        |
| GET    | `/api/v1/audit-logs`   | 200           | Audit log listing        |
| GET    | `/api/v1/metrics`      | 200           | System metrics           |
| GET    | `/api/v1/debug/whoami` | 200           | Debug: current user info |
| GET    | `/api/v1/users`        | 200           | List users               |
| GET    | `/api/v1/users/{id}`   | 200, 404      | Get user                 |
| POST   | `/api/v1/users`        | 201, 400      | Create user              |
| GET    | `/health`              | 200           | Health check             |

---

## 3. Employee Service (Port 8083)

### EmployeeController — `/api/v1/employees`

| Method | Endpoint            | Status Codes       | Description                  |
| ------ | ------------------- | ------------------ | ---------------------------- |
| GET    | `/`                 | 200                | List employees               |
| GET    | `/{id}`             | 200, 404           | Get employee                 |
| GET    | `/by-user/{userId}` | 200, 404           | Get employee by user ID      |
| POST   | `/`                 | 201, 400           | Create employee              |
| POST   | `/{id}/activate`    | 200, 400, 404, 409 | Activate employee            |
| POST   | `/{id}/terminate`   | 200, 400, 404, 409 | Terminate employee           |
| GET    | `/changed-since`    | 200                | Employees changed since date |
| GET    | `/payroll-summary`  | 200                | Payroll summary              |

### HealthCheckController

| Method | Endpoint                     | Status Codes | Description     |
| ------ | ---------------------------- | ------------ | --------------- |
| GET    | `/actuator/health`           | 200          | Actuator health |
| GET    | `/health`                    | 200          | Service health  |
| GET    | `/api/v1/health`             | 200          | API health      |
| GET    | `/actuator/health/readiness` | 200          | Readiness probe |
| GET    | `/actuator/health/liveness`  | 200          | Liveness probe  |

---

## 4. Payment Service (Port 8084)

### PaymentController — `/api/v1/payments`

| Method | Endpoint             | Status Codes       | Description               |
| ------ | -------------------- | ------------------ | ------------------------- |
| GET    | `/`                  | 200                | List payments (paginated) |
| GET    | `/{id}`              | 200, 404           | Get payment               |
| GET    | `/tenant/{tenantId}` | 200, 404           | Payments by tenant        |
| GET    | `/lease/{leaseId}`   | 200, 404           | Payments by lease         |
| POST   | `/`                  | 201, 400           | Create payment            |
| POST   | `/{id}/process`      | 200, 400, 404, 409 | Process payment           |
| POST   | `/{id}/refund`       | 200, 400, 404, 409 | Refund payment            |
| PATCH  | `/{id}`              | 200, 400, 404      | Update payment            |

### StripePaymentController — `/api/v1/stripe-payments`

| Method | Endpoint                      | Status Codes  | Description          |
| ------ | ----------------------------- | ------------- | -------------------- |
| POST   | `/charge`                     | 200, 400, 500 | Create Stripe charge |
| GET    | `/payment-intents/{intentId}` | 200, 404, 500 | Get payment intent   |

### StripeCustomerController — `/api/v1/stripe-customers`

| Method | Endpoint        | Status Codes  | Description            |
| ------ | --------------- | ------------- | ---------------------- |
| POST   | `/`             | 201, 400, 500 | Create Stripe customer |
| GET    | `/{customerId}` | 200, 404, 500 | Get Stripe customer    |

### PaymentMethodController — `/api/v1/payment-methods`

| Method | Endpoint | Status Codes | Description           |
| ------ | -------- | ------------ | --------------------- |
| GET    | `/`      | 200          | List payment methods  |
| POST   | `/`      | 201, 400     | Add payment method    |
| DELETE | `/{id}`  | 204, 404     | Delete payment method |

### PromoCodeController — `/api/v1/promo-codes`

| Method | Endpoint    | Status Codes  | Description         |
| ------ | ----------- | ------------- | ------------------- |
| GET    | `/`         | 200           | List promo codes    |
| POST   | `/`         | 201, 400      | Create promo code   |
| POST   | `/validate` | 200, 400, 404 | Validate promo code |

### PaymentReminderController — `/api/v1/payment-reminders`

| Method | Endpoint     | Status Codes       | Description     |
| ------ | ------------ | ------------------ | --------------- |
| GET    | `/`          | 200                | List reminders  |
| POST   | `/`          | 201, 400           | Create reminder |
| POST   | `/send/{id}` | 200, 400, 404, 503 | Send reminder   |

### TransactionHistoryController — `/api/v1/transaction-history`

| Method | Endpoint | Status Codes | Description       |
| ------ | -------- | ------------ | ----------------- |
| GET    | `/`      | 200          | List transactions |
| GET    | `/{id}`  | 200, 404     | Get transaction   |

### ApplicationFeeController — `/api/v1/application-fees`

| Method | Endpoint  | Status Codes  | Description            |
| ------ | --------- | ------------- | ---------------------- |
| GET    | `/`       | 200           | List fees              |
| POST   | `/`       | 201, 400      | Create fee             |
| POST   | `/charge` | 200, 400, 500 | Charge application fee |

### OrganizationApplicationFeeController — `/api/v1/org-application-fees`

| Method | Endpoint   | Status Codes  | Description        |
| ------ | ---------- | ------------- | ------------------ |
| GET    | `/{orgId}` | 200, 404      | Get org fee config |
| POST   | `/{orgId}` | 201, 400, 409 | Set org fee config |

### PaymentContextController — `/api/v1/payment-context`

| Method | Endpoint | Status Codes | Description         |
| ------ | -------- | ------------ | ------------------- |
| GET    | `/{id}`  | 200, 404     | Get payment context |

### StripeWebhookController — `/webhooks/stripe`

| Method | Endpoint | Status Codes | Description            |
| ------ | -------- | ------------ | ---------------------- |
| POST   | `/`      | 200, 400     | Process Stripe webhook |

---

## 5. Payroll Service (Port 8085)

### PayrollController — `/api/v1/clients/{clientId}/payroll`

| Method | Endpoint               | Status Codes       | Description                   |
| ------ | ---------------------- | ------------------ | ----------------------------- |
| GET    | `/`                    | 200                | List payroll runs (paginated) |
| GET    | `/{payrollId}`         | 200, 404           | Get payroll run               |
| POST   | `/`                    | 201, 400           | Create payroll run            |
| POST   | `/{payrollId}/process` | 200, 400, 404, 409 | Process payroll               |
| POST   | `/{payrollId}/approve` | 200, 400, 404, 409 | Approve payroll               |

### TimesheetController — `/api/v1/clients/{clientId}/timesheets`

| Method | Endpoint                 | Status Codes       | Description         |
| ------ | ------------------------ | ------------------ | ------------------- |
| GET    | `/`                      | 200                | List timesheets     |
| GET    | `/{id}`                  | 200, 404           | Get timesheet       |
| GET    | `/employee/{employeeId}` | 200, 404           | Employee timesheets |
| POST   | `/{id}/submit`           | 200, 400, 404, 409 | Submit timesheet    |
| POST   | `/{id}/approve`          | 200, 400, 404, 409 | Approve timesheet   |
| POST   | `/{id}/reject`           | 200, 400, 404, 409 | Reject timesheet    |
| GET    | `/pending/{clientId}`    | 200                | Pending timesheets  |

### EmployeeEntityController — `/api/v1/employees`

| Method | Endpoint                      | Status Codes       | Description            |
| ------ | ----------------------------- | ------------------ | ---------------------- |
| POST   | `/`                           | 201, 400           | Create employee        |
| GET    | `/{id}`                       | 200, 404           | Get employee           |
| GET    | `/by-number/{employeeNumber}` | 200, 404           | Get by employee number |
| GET    | `/client/{clientId}`          | 200, 404           | Employees by client    |
| GET    | `/client/{clientId}/active`   | 200                | Active employees       |
| GET    | `/client/{clientId}/search`   | 200                | Search employees       |
| PUT    | `/{id}`                       | 200, 400, 404      | Update employee        |
| POST   | `/{id}/terminate`             | 200, 400, 404, 409 | Terminate employee     |

### EmployeeController — `/api/v1/clients/{clientId}/employees`

| Method | Endpoint          | Status Codes       | Description         |
| ------ | ----------------- | ------------------ | ------------------- |
| GET    | `/`               | 200                | List employees      |
| GET    | `/{id}`           | 200, 404           | Get employee        |
| POST   | `/`               | 201, 400           | Create employee     |
| PUT    | `/{id}`           | 200, 400, 404      | Update employee     |
| POST   | `/{id}/terminate` | 200, 400, 404, 409 | Terminate employee  |
| POST   | `/{id}/sync`      | 200, 400, 404      | Sync employee data  |
| POST   | `/sync`           | 200, 400           | Bulk sync employees |

### TimeEntryController — `/api/v1/time-entries`

| Method | Endpoint                       | Status Codes       | Description                  |
| ------ | ------------------------------ | ------------------ | ---------------------------- |
| POST   | `/`                            | 201, 400           | Create time entry            |
| GET    | `/{id}`                        | 200, 404           | Get time entry               |
| GET    | `/employee/{employeeId}`       | 200, 404           | Employee time entries        |
| GET    | `/employee/{employeeId}/range` | 200, 404           | Time entries by date range   |
| GET    | `/client/{clientId}/range`     | 200, 404           | Client time entries by range |
| GET    | `/client/{clientId}/pending`   | 200, 404           | Pending client entries       |
| POST   | `/{id}/approve`                | 200, 400, 404, 409 | Approve time entry           |
| POST   | `/{id}/reject`                 | 200, 400, 404, 409 | Reject time entry            |
| DELETE | `/{id}`                        | 204, 404           | Delete time entry            |
| GET    | `/{employeeId}/hours`          | 200, 404           | Total employee hours         |

### CompensationController — `/api/v1/compensation`

| Method | Endpoint | Status Codes | Description         |
| ------ | -------- | ------------ | ------------------- |
| POST   | `/`      | 201, 400     | Create compensation |
| GET    | `/`      | 200          | List compensations  |
| GET    | `/{id}`  | 200, 404     | Get compensation    |

### LeaveController — `/api/v1/leave`

| Method | Endpoint        | Status Codes       | Description        |
| ------ | --------------- | ------------------ | ------------------ |
| GET    | `/`             | 200                | List leave records |
| POST   | `/`             | 201, 400           | Request leave      |
| GET    | `/{id}`         | 200, 404           | Get leave record   |
| POST   | `/{id}/approve` | 200, 400, 404, 409 | Approve leave      |
| POST   | `/{id}/reject`  | 200, 400, 404, 409 | Reject leave       |

### DepartmentController — `/api/v1/departments`

| Method | Endpoint | Status Codes  | Description       |
| ------ | -------- | ------------- | ----------------- |
| GET    | `/`      | 200           | List departments  |
| POST   | `/`      | 201, 400      | Create department |
| GET    | `/{id}`  | 200, 404      | Get department    |
| PUT    | `/{id}`  | 200, 400, 404 | Update department |

### ClientController — `/api/v1/clients`

| Method | Endpoint      | Status Codes | Description   |
| ------ | ------------- | ------------ | ------------- |
| GET    | `/`           | 200          | List clients  |
| GET    | `/{clientId}` | 200, 404     | Get client    |
| POST   | `/`           | 201, 400     | Create client |

---

## 6. API Gateway (Port 8080)

### TokenController — `/api/v1/gateway`

| Method | Endpoint            | Status Codes | Description             |
| ------ | ------------------- | ------------ | ----------------------- |
| POST   | `/token/validate`   | 200, 400     | Validate JWT token      |
| POST   | `/token/introspect` | 200, 400     | Introspect token claims |

### CircuitBreakerMonitoringController — `/api/v1/gateway`

| Method | Endpoint                  | Status Codes | Description            |
| ------ | ------------------------- | ------------ | ---------------------- |
| GET    | `/circuit-breaker/status` | 200          | Circuit breaker status |
| GET    | `/circuit-breaker/health` | 200          | Circuit breaker health |

### FallbackController

| Method | Endpoint          | Status Codes | Description                 |
| ------ | ----------------- | ------------ | --------------------------- |
| ALL    | `/**` (unmatched) | 404, 503     | Fallback for unmapped paths |

---

## 7. Python Services

### Report Service (Port 8090)

| Method | Endpoint                     | Status Codes  | Description                |
| ------ | ---------------------------- | ------------- | -------------------------- |
| GET    | `/reports/financial/pdf`     | 200, 400, 500 | Financial report (PDF)     |
| GET    | `/reports/delinquency/excel` | 200, 400, 500 | Delinquency report (Excel) |
| GET    | `/reports/rent-roll/excel`   | 200, 400, 500 | Rent roll report (Excel)   |
| GET    | `/health`                    | 200           | Health check               |

### Document Service (Port 8092)

| Method | Endpoint                   | Status Codes  | Description                |
| ------ | -------------------------- | ------------- | -------------------------- |
| POST   | `/documents/upload`        | 200, 400, 503 | Upload document to MinIO   |
| GET    | `/documents/url`           | 200, 404      | Get presigned download URL |
| DELETE | `/documents/{object_name}` | 200, 404      | Delete document            |
| GET    | `/health`                  | 200, 503      | Health check               |

### Search Reranker (Port 8093)

| Method | Endpoint  | Status Codes | Description           |
| ------ | --------- | ------------ | --------------------- |
| POST   | `/rerank` | 200, 400     | BM25 search reranking |
| GET    | `/health` | 200          | Health check          |

### Vendor Matching (Port 8091)

| Method | Endpoint         | Status Codes | Description              |
| ------ | ---------------- | ------------ | ------------------------ |
| POST   | `/match-vendors` | 200, 400     | Semantic vendor matching |
| GET    | `/health`        | 200          | Health check             |

---

## 8. Service Registry (Port 8761)

### RegistryInfoController

| Method | Endpoint  | Status Codes | Description              |
| ------ | --------- | ------------ | ------------------------ |
| GET    | `/apps`   | 200          | List registered services |
| GET    | `/health` | 200          | Registry health check    |

---

## Summary Statistics

| Service            | Endpoints | Controllers |
| ------------------ | --------- | ----------- |
| Auth Service       | ~85       | 5           |
| Propertize Service | ~120      | 20+         |
| Employee Service   | ~13       | 2           |
| Payment Service    | ~30       | 10          |
| Payroll Service    | ~45       | 8           |
| API Gateway        | ~5        | 3           |
| Python Services    | ~10       | 4           |
| Service Registry   | ~2        | 1           |
| **Total**          | **~310+** | **53+**     |
