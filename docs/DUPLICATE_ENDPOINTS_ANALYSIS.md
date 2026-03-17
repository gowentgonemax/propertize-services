# Duplicate Endpoints Analysis - Propertize Service

## Overview

After migrating authentication and user management to `auth-service`, the following endpoints in `propertize-service` are now **DUPLICATES** and should be **REMOVED**.

---

## 🔴 FILES TO DELETE

### 1. **AuthController.java**

**Path:** `/propertize/src/main/java/com/propertize/controller/AuthController.java`

**Reason:** All authentication endpoints have been migrated to auth-service. This entire controller is duplicate.

**Duplicate Endpoints:**

```
POST   /api/v1/auth/login                    → Already in auth-service
POST   /api/v1/auth/refresh                  → Already in auth-service
POST   /api/v1/auth/logout                   → Already in auth-service
POST   /api/v1/auth/forgot-password          → Already in auth-service
POST   /api/v1/auth/reset-password           → Already in auth-service
GET    /api/v1/auth/validate-reset-token     → Already in auth-service
POST   /api/v1/auth/change-password          → Already in auth-service
GET    /api/v1/auth/me                       → Already in auth-service
GET    /api/v1/auth/permissions              → Already in auth-service
POST   /api/v1/auth/validate-session         → Already in auth-service
POST   /api/v1/auth/validate-token           → Already in auth-service
GET    /api/v1/auth/diagnostic/token-check   → Diagnostic endpoint
```

---

### 2. **UserController.java**

**Path:** `/propertize/src/main/java/com/propertize/controller/UserController.java`

**Reason:** User management (CRUD operations, role management, account management) should be centralized in auth-service to avoid conflicts and ensure single source of truth for user data.

**Duplicate Endpoints:**

```
POST   /api/v1/users                                         → Create user
GET    /api/v1/users/{userId}                                → Get user by ID
GET    /api/v1/users                                         → List all users
GET    /api/v1/users/role/{role}                             → Get users by role
GET    /api/v1/users/active                                  → Get active users
GET    /api/v1/users/locked                                  → Get locked users
PUT    /api/v1/users/{userId}                                → Update user
POST   /api/v1/users/{userId}/roles/{role}                   → Add role to user
DELETE /api/v1/users/{userId}/roles/{role}                   → Remove role from user
POST   /api/v1/users/{userId}/lock                           → Lock user account
POST   /api/v1/users/{userId}/unlock                         → Unlock user account
POST   /api/v1/users/{userId}/disable                        → Disable user
POST   /api/v1/users/{userId}/enable                         → Enable user
POST   /api/v1/users/{userId}/reset-password                 → Reset password
DELETE /api/v1/users/{userId}                                → Delete user
POST   /api/v1/users/bulk-disable                            → Bulk disable users
GET    /api/v1/users/stats/count                             → Get user statistics
PUT    /api/v1/users/{username}/associate-organization/{organizationId}  → Associate user with org
```

---

## ⚠️ SERVICES TO REMOVE/REFACTOR

### Services with User Creation Logic

#### 1. **OrganizationService.java**

**Path:** `/propertize/src/main/java/com/propertize/services/OrganizationService.java`

**Method to refactor:** `createOrganizationWithOwner()`

- **Issue:** Creates User entities directly using `userRepository.save()`
- **Solution:** Call auth-service API to create user instead

#### 2. **OnboardingService.java**

**Path:** `/propertize/src/main/java/com/propertize/services/OnboardingService.java`

**Method to refactor:** `createOwnerUser()`

- **Issue:** Creates User entities directly using `userRepository.save()`
- **Solution:** Call auth-service API to create user instead

#### 3. **UserService.java**

**Path:** `/propertize/src/main/java/com/propertize/services/UserService.java`

**Action:** **DELETE ENTIRE FILE**

- Contains all user CRUD logic which duplicates auth-service functionality
- Methods include: createUser, updateUser, deleteUser, lockUser, unlockUser, addRoleToUser, removeRoleFromUser, etc.

---

## ✅ RECOMMENDED ACTION PLAN

### Step 1: Remove Duplicate Controllers

```bash
rm propertize/src/main/java/com/propertize/controller/AuthController.java
rm propertize/src/main/java/com/propertize/controller/UserController.java
```

### Step 2: Remove UserService

```bash
rm propertize/src/main/java/com/propertize/services/UserService.java
```

### Step 3: Create Auth-Service Client

Create a new service to communicate with auth-service for user operations:

```java
@Service
public class AuthServiceClient {
    private final RestTemplate restTemplate;
    private final String authServiceUrl = "http://auth-service:8081";

    public UserResponse createUser(UserCreateRequest request) {
        return restTemplate.postForObject(
            authServiceUrl + "/api/v1/users",
            request,
            UserResponse.class
        );
    }

    // Add other methods as needed
}
```

### Step 4: Refactor Organization/Onboarding Services

Replace direct user creation with auth-service API calls:

```java
// OLD (in OrganizationService)
User savedOwner = userRepository.save(owner);

// NEW
UserResponse savedOwner = authServiceClient.createUser(userRequest);
```

### Step 5: Update Bruno Collection

Remove duplicate auth/user endpoints from propertize bruno collection and ensure they exist in auth-service collection.

---

## 📋 VERIFICATION CHECKLIST

- [ ] Delete `AuthController.java` from propertize-service
- [ ] Delete `UserController.java` from propertize-service
- [ ] Delete `UserService.java` from propertize-service
- [ ] Create `AuthServiceClient` for inter-service communication
- [ ] Refactor `OrganizationService.createOrganizationWithOwner()` to use auth-service API
- [ ] Refactor `OnboardingService.createOwnerUser()` to use auth-service API
- [ ] Remove UserRepository usage from organization/onboarding services (except for read-only operations)
- [ ] Update Bruno collection - remove duplicate endpoints
- [ ] Test organization creation flow with auth-service integration
- [ ] Test onboarding application approval flow
- [ ] Rebuild and restart propertize-service
- [ ] Verify no 404 errors on `/api/v1/admin/organizations/applications`

---

## 🔍 ROOT CAUSE OF CURRENT ISSUE

**Error:**

```json
{
  "timestamp": "2026-02-16T16:08:34.201+00:00",
  "path": "/admin/organizations/applications",
  "status": 404,
  "error": "Not Found",
  "requestId": "6b3b00e6-227"
}
```

**Analysis:**
The 404 error is likely caused by conflicting endpoints and service initialization issues due to duplicate user management logic in both services. When propertize-service tries to create users directly, it conflicts with auth-service's authority over user management.

**Solution:**
Remove all user management from propertize-service and delegate to auth-service via REST API calls.

---

## 🎯 EXPECTED OUTCOME

After removing duplicates:

1. ✅ Single source of truth for user management (auth-service)
2. ✅ No conflicts between services
3. ✅ Propertize service focuses on business logic (organizations, properties, leases, etc.)
4. ✅ Auth service owns authentication, authorization, and user CRUD
5. ✅ Clean separation of concerns
6. ✅ `/api/v1/admin/organizations/applications` endpoint works correctly

---

## 📝 NOTES

- Keep read-only operations like `UserRepository.findByUsername()` in propertize for authorization checks
- Auth-service should expose API endpoints for user creation/management
- Use RestTemplate or WebClient for inter-service communication
- Consider implementing circuit breaker pattern for resilience
- All user writes must go through auth-service to maintain data integrity
