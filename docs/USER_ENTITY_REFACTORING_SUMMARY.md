# User Entity Refactoring Summary

## Overview

Successfully removed duplicate User entity from propertize service. The User entity now exists ONLY in auth-service.

## Completed Changes

### 1. Entity Updates ✅

- **SupportTicket**: Replaced `User createdBy` and `User assignedTo` with `Long createdByUserId` and `Long assignedToUserId`
- **Organization**: Removed `User owner` relationship (kept `Long ownerUserId`)
- **Payment**: Replaced `User owner` and `User processedByUser` with `Long ownerId` and `Long processedByUserId`
- **Invoice**: Replaced `User createdByUser` with `Long createdByUserId`
- **LeaseAmendment**: Replaced `User createdByUser` and `User approvedByUser` with `Long createdByUserId` and `Long approvedByUserId`
- **PropertyManagement**: Replaced `User assignedManager` with `Long assignedManagerId`
- **Property**: Removed `User listingAgent` relationship (kept `Long listingAgentId`)

### 2. Deleted Files ✅

- `/propertize/src/main/java/com/propertize/entity/User.java`
- `/propertize/src/main/java/com/propertize/repository/UserRepository.java`

### 3. Repository Updates ✅

- **SupportTicketRepository**: Updated query methods to use field names instead of navigation (e.g., `findByCreatedByUserId` instead of `findByCreatedBy_Id`)

### 4. Service Updates (Partial) ✅

- **SupportTicketService**: Removed UserRepository injection, added TODOs for auth-service calls

## Remaining Work

### Files That Need Fixing (100 compilation errors)

The following files still reference User entity or UserRepository and need to be updated:

#### Services (Priority: HIGH)

1. **UserService.java** - Should be moved to auth-service or become a REST client wrapper
2. **OrganizationManagementService.java** - Remove User queries, use auth-service
3. **OrganizationService.java** - Remove User queries
4. **OnboardingService.java** - Remove User creation logic, delegate to auth-service
5. **PaymentContextService.java** - Remove UserRepository
6. **MessageService.java** - Remove User queries
7. **ApprovalWorkflowService.java** - Remove User queries
8. **DocumentService.java** - Remove User queries
9. **InvoiceService.java** - Remove User queries
10. **RentalApplicationService.java** - Remove UserRepository
11. **LeaseSignatureService.java** - Remove User queries
12. **TaskService.java** - Remove User queries
13. **OrganizationInfoService.java** - Remove UserRepository
14. **ScheduleEventService.java** - Remove User references

#### Dashboard Services (Priority: MEDIUM)

15. **AccountantDashboardService.java**
16. **TechnicianDashboardService.java**
17. **AgentDashboardService.java**
18. **LandlordDashboardService.java**
19. **SupervisorDashboardService.java**
20. **ManagerDashboardService.java**

#### Controllers (Priority: HIGH)

21. **MessageController.java** - Remove UserRepository
22. **OrganizationController.java** - Remove User references

#### Mappers (Priority: HIGH)

23. **OrganizationMapper.java** - Remove User parameter from mapping methods
24. **PaymentMapper.java** - Remove User parameter

#### Utilities & Security (Priority: CRITICAL)

25. **SecurityUtils.java** - Remove UserRepository, get user info from SecurityContext
26. **OrganizationSecurityValidator.java** - Remove UserRepository
27. **SecurityAuditAspect.java** - Remove User references

#### Interceptors (Priority: HIGH)

28. **OrganizationStatusInterceptor.java** - Remove UserRepository
29. **OrganizationFilterInterceptor.java** - Remove User references

## Recommended Approach

### Phase 1: Create AuthServiceClient (Feign Client)

```java
@FeignClient(name = "auth-service", url = "${auth.service.url}")
public interface AuthServiceClient {

    @GetMapping("/api/users/{userId}")
    UserDTO getUserById(@PathVariable Long userId);

    @GetMapping("/api/users/username/{username}")
    UserDTO getUserByUsername(@PathVariable String username);

    @GetMapping("/api/users/email/{email}")
    UserDTO getUserByEmail(@PathVariable String email);

    @GetMapping("/api/users/organization/{organizationId}")
    List<UserDTO> getUsersByOrganizationId(@PathVariable String organizationId);
}
```

### Phase 2: Update Service Pattern

For each service that needs User data:

1. Remove `UserRepository` injection
2. Add `AuthServiceClient` injection (or create a UserServiceClient wrapper)
3. Replace `userRepository.findById(id)` with `authServiceClient.getUserById(id)`
4. Replace `userRepository.findByUsername(username)` with `authServiceClient.getUserByUsername(username)`
5. Add proper error handling for service-to-service calls

### Phase 3: Update Mappers

For MapStruct mappers:

1. Remove `User` parameters from mapping methods
2. Use `Long userId` fields directly
3. If User details needed in DTOs, fetch separately from auth-service

### Phase 4: Fix Security & Utils

1. **SecurityUtils**: Get user info from JWT claims or SecurityContext
2. **OrganizationSecurityValidator**: Call auth-service to validate user access
3. **Interceptors**: Use JWT claims instead of database queries

## Special Cases

### UserService

This service manages User CRUD operations and should either:

- Option A: Be moved entirely to auth-service
- Option B: Become a thin REST client wrapper around auth-service endpoints
- **Recommendation**: Option B - Keep it as a client wrapper for backwards compatibility

### OnboardingService

Creates users during onboarding. Should:

- Call auth-service API to create users
- Store only userId references in propertize tables

### Organization Entity Methods

The Organization entity has methods like:

- `getOwner()` - Returns User (line 184)
- `setOwner(User user)` - Sets User (line 184)

These need to be removed since we only keep ownerUserId field.

## Database Impact

No schema changes needed! All foreign key columns (user_id fields) already exist as primitive Long types.

## Next Steps

1. Create AuthServiceClient Feign interface
2. Fix critical files first (SecurityUtils, Interceptors)
3. Update all services systematically
4. Update mappers
5. Remove Organization.getOwner()/setOwner() methods
6. Test compilation
7. Update tests

## Notes

- The auth-service User entity remains unchanged
- All user authentication and RBAC logic stays in auth-service
- Propertize service will fetch user data via REST calls when needed
- Consider caching user data to reduce service-to-service calls
