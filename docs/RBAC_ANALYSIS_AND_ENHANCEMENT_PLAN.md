# RBAC System Analysis & Enhancement Plan

## Executive Summary

Your current RBAC (Role-Based Access Control) system is **well-structured** with good foundations, but there are significant opportunities to make it **more granular and dynamic**. This document provides a comprehensive analysis and actionable enhancement plan.

---

## Current RBAC Architecture

### ✅ Strengths

1. **YAML-Based Configuration** ([rbac.yml](propertize/src/main/resources/rbac.yml))
   - Centralized permission management
   - Easy to modify without code changes
   - Version-controlled role definitions

2. **Hierarchical Role System**
   - 5 scope levels: Platform → Portfolio → Organization → Team → Self
   - Level-based hierarchy (1000 → 100)
   - Clear organizational boundaries

3. **Permission Templates** (Lines 23-300 in rbac.yml)
   - Reusable permission sets (e.g., `full_crud`, `financial_management`)
   - Reduces duplication
   - Consistent permission patterns

4. **Role Inheritance**
   - Roles can inherit from parent roles
   - Reduces redundancy in permission definitions
   - Simplifies role management

5. **Spring Security Integration**
   - JWT-based authentication
   - @PreAuthorize annotations support
   - GrantedAuthority mapping

6. **Frontend RBAC Service** ([propertize-front-end/src/utils/rbac.ts](propertize-front-end/src/utils/rbac.ts))
   - Permission checking utilities
   - Role-based UI rendering
   - Consistent with backend

---

## 🔍 Current Limitations

### 1. **Static Role Assignments**

```java
// Current: Users get ONE role
user.setRole(UserRoleEnum.PROPERTY_MANAGER);

// Limitation: Cannot have multiple roles simultaneously
// e.g., User cannot be both PROPERTY_MANAGER and LEASING_AGENT
```

**Impact**: Rigid user capabilities, requires creating hybrid roles

---

### 2. **No Context-Aware Permissions**

```yaml
# Current
property:update # Same permission regardless of property ownership
```

**Limitation**: Cannot check:

- "Can user update THEIR OWN properties only?"
- "Can user update properties in THEIR portfolio?"
- "Can user update properties where they are assigned?"

**Impact**: Over-permissive or requires custom code checks

---

### 3. **No Resource-Level Permissions**

```java
// Current approach
@PreAuthorize("hasAuthority('property:update')")
public Property updateProperty(Long propertyId, PropertyDTO dto) {
    // No automatic check if user owns/manages this specific property
}
```

**Limitation**: All users with `property:update` can update ALL properties in their scope

**Impact**: Requires manual ownership checks in every service method

---

### 4. **No Attribute-Based Access Control (ABAC)**

```yaml
# Current: Role → Permissions
# Missing: Dynamic rules based on attributes

# Cannot express:
- "Property managers can approve expenses < $5000"
- "Leasing agents can only view properties they're assigned to"
- "Maintenance can update tickets during business hours only"
```

---

### 5. **No Dynamic Permission Conditions**

```java
// Want to express but can't:
@PreAuthorize("hasPermission('property', propertyId, 'update') AND property.value < 1000000")
@PreAuthorize("hasRole('LEASING_AGENT') AND user.region == property.region")
```

---

### 6. **No Time-Based Permissions**

```yaml
# Cannot express:
- Temporary elevated privileges
- Emergency access with auto-expiration
- Business hours restrictions
- Vacation/absence delegation
```

---

### 7. **Limited Multi-Tenancy Support**

```java
// Current organization scope
authorities.add(new SimpleGrantedAuthority("ORG_" + organizationId));

// Limitation: User can only belong to ONE organization
// Cannot support:
- Users working across multiple organizations
- Consultants with access to multiple clients
- Shared service providers
```

---

### 8. **No Permission Delegation**

```yaml
# Cannot express:
- Manager temporarily delegates approval authority to assistant
- Property owner delegates management to property manager
- Vacation coverage with time-limited permissions
```

---

### 9. **No Field-Level Security**

```java
// Current: All-or-nothing access
property:read → Full property details

// Missing:
- Leasing agent sees: address, photos, availability
- Accountant sees: rent, expenses, financials
- Maintenance sees: address, condition, work orders
```

---

### 10. **No Conditional Role Activation**

```yaml
# Cannot express:
ORGANIZATION_OWNER:
  active_when:
    - organization.status == "ACTIVE"
    - user.verification == "COMPLETED"
    - subscription.plan in ["PREMIUM", "ENTERPRISE"]
```

---

## 🚀 Enhancement Recommendations

### **Phase 1: Multi-Role Support (High Priority)**

#### Backend Changes

**1.1 Update User Entity**

```java
// File: User.java
@Entity
public class User {
    // Change from single role
    // @Enumerated(EnumType.STRING)
    // private UserRoleEnum role;

    // To multiple roles
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles",
                    joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private Set<UserRoleEnum> roles = new HashSet<>();

    // Primary role for backwards compatibility
    @Transient
    public UserRoleEnum getPrimaryRole() {
        return roles.stream()
            .max(Comparator.comparing(r -> getRoleLevel(r)))
            .orElse(null);
    }

    // Helper methods
    public void addRole(UserRoleEnum role) {
        this.roles.add(role);
    }

    public void removeRole(UserRoleEnum role) {
        this.roles.remove(role);
    }

    public boolean hasRole(UserRoleEnum role) {
        return this.roles.contains(role);
    }

    public boolean hasAnyRole(UserRoleEnum... roles) {
        return Arrays.stream(roles).anyMatch(this.roles::contains);
    }
}
```

**1.2 Update JWT Token Generation**

```java
// File: JwtService.java
public String generateToken(User user) {
    // Include ALL roles in JWT
    List<String> roleNames = user.getRoles().stream()
        .map(UserRoleEnum::name)
        .collect(Collectors.toList());

    return Jwts.builder()
        .setSubject(user.getUsername())
        .claim("roles", roleNames) // Multiple roles
        .claim("primaryRole", user.getPrimaryRole().name())
        .claim("organizationId", user.getOrganizationId())
        // ... other claims
        .signWith(privateKey, SignatureAlgorithm.RS256)
        .compact();
}
```

**1.3 Update Authentication Filter**

```java
// File: JwtAuthenticationFilter.java
private Collection<GrantedAuthority> loadRbacAuthorities(Set<String> roles, String username) {
    Set<GrantedAuthority> authorities = new HashSet<>();

    // Load permissions for ALL roles
    roles.forEach(roleName -> {
        // Add role authority
        authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName));
        authorities.add(new SimpleGrantedAuthority(roleName));

        // Load permissions for this role
        Set<String> permissions = rbacService.getPermissionsForRole(roleName);
        permissions.forEach(permission ->
            authorities.add(new SimpleGrantedAuthority(permission))
        );
    });

    return authorities;
}
```

**1.4 Update Database Migration**

```sql
-- Migration: Add user_roles table
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role VARCHAR(100) NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    assigned_by BIGINT,
    expires_at TIMESTAMP NULL,
    is_active BOOLEAN DEFAULT true,

    PRIMARY KEY (user_id, role),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (assigned_by) REFERENCES users(id) ON DELETE SET NULL,

    INDEX idx_user_roles_user_id (user_id),
    INDEX idx_user_roles_active (is_active)
);

-- Migrate existing single role to multi-role
INSERT INTO user_roles (user_id, role, is_active)
SELECT id, role, true FROM users WHERE role IS NOT NULL;
```

---

### **Phase 2: Resource-Level Permissions (High Priority)**

#### 2.1 Add Permission Evaluator

```java
// File: ResourcePermissionEvaluator.java
@Component
public class ResourcePermissionEvaluator implements PermissionEvaluator {

    @Autowired private PropertyService propertyService;
    @Autowired private SecurityService securityService;
    @Autowired private RbacService rbacService;

    @Override
    public boolean hasPermission(Authentication authentication,
                                Object targetDomainObject,
                                Object permission) {
        if (authentication == null || permission == null) {
            return false;
        }

        String permissionStr = permission.toString();

        // Check base permission first
        if (!hasAuthority(authentication, permissionStr)) {
            return false;
        }

        // Resource-specific checks
        if (targetDomainObject instanceof Property) {
            return canAccessProperty(authentication, (Property) targetDomainObject, permissionStr);
        } else if (targetDomainObject instanceof Lease) {
            return canAccessLease(authentication, (Lease) targetDomainObject, permissionStr);
        }

        return true; // Default allow if no specific check
    }

    @Override
    public boolean hasPermission(Authentication authentication,
                                Serializable targetId,
                                String targetType,
                                Object permission) {
        if (authentication == null || permission == null) {
            return false;
        }

        // Load resource and check
        if ("Property".equals(targetType)) {
            Property property = propertyService.findById((Long) targetId);
            return hasPermission(authentication, property, permission);
        }

        return false;
    }

    private boolean canAccessProperty(Authentication auth, Property property, String permission) {
        String username = auth.getName();
        User user = securityService.getCurrentUser();

        // Platform admins can access everything
        if (rbacService.isPlatformAdmin(auth.getAuthorities())) {
            return true;
        }

        // Organization owner can access org properties
        if (hasAuthority(auth, "ORGANIZATION_OWNER") &&
            property.getOrganizationId().equals(user.getOrganizationId())) {
            return true;
        }

        // Property manager can access assigned properties
        if (hasAuthority(auth, "PROPERTY_MANAGER")) {
            return propertyService.isUserAssignedToProperty(user.getId(), property.getId());
        }

        // Leasing agent can access properties in their region
        if (hasAuthority(auth, "LEASING_AGENT")) {
            return property.getRegion().equals(user.getRegion());
        }

        return false;
    }

    private boolean hasAuthority(Authentication auth, String authority) {
        return auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals(authority) ||
                          a.getAuthority().equals("ROLE_" + authority));
    }
}
```

#### 2.2 Enable in Security Config

```java
// File: SecurityConfig.java
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Autowired
    private ResourcePermissionEvaluator permissionEvaluator;

    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler() {
        DefaultMethodSecurityExpressionHandler handler =
            new DefaultMethodSecurityExpressionHandler();
        handler.setPermissionEvaluator(permissionEvaluator);
        return handler;
    }
}
```

#### 2.3 Use in Controllers

```java
// File: PropertyController.java
@RestController
public class PropertyController {

    // NEW: Resource-level permission check
    @PreAuthorize("hasPermission(#propertyId, 'Property', 'property:update')")
    @PutMapping("/properties/{propertyId}")
    public Property updateProperty(@PathVariable Long propertyId,
                                   @RequestBody PropertyDTO dto) {
        // Automatic check if user can update THIS specific property
        return propertyService.update(propertyId, dto);
    }

    // NEW: Object-level permission check
    @PreAuthorize("hasPermission(#property, 'property:delete')")
    @DeleteMapping("/properties/{propertyId}")
    public void deleteProperty(@PathVariable Long propertyId) {
        Property property = propertyService.findById(propertyId);
        propertyService.delete(property);
    }
}
```

---

### **Phase 3: Attribute-Based Access Control (ABAC)**

#### 3.1 Add RBAC Rules Engine

```yaml
# File: rbac.yml - Add conditional rules
roles:
  PROPERTY_MANAGER:
    # ... existing config
    conditionalPermissions:
      # Can approve expenses under $5000
      - permission: "expense:approve"
        conditions:
          - attribute: "expense.amount"
            operator: "LESS_THAN"
            value: 5000

      # Can only update properties they manage
      - permission: "property:update"
        conditions:
          - attribute: "property.managerId"
            operator: "EQUALS"
            value: "@user.id"

      # Can only view tenants in their properties
      - permission: "tenant:read"
        conditions:
          - attribute: "tenant.propertyId"
            operator: "IN"
            value: "@user.managedPropertyIds"

  LEASING_AGENT:
    conditionalPermissions:
      # Can only schedule showings during business hours
      - permission: "showing:schedule"
        conditions:
          - attribute: "showing.time"
            operator: "BETWEEN"
            value: ["09:00", "18:00"]
          - attribute: "showing.day"
            operator: "IN"
            value: ["MON", "TUE", "WED", "THU", "FRI"]

      # Can only access properties in their region
      - permission: "property:view"
        conditions:
          - attribute: "property.region"
            operator: "EQUALS"
            value: "@user.region"
```

#### 3.2 Implement Rules Engine

```java
// File: RbacRulesEngine.java
@Service
public class RbacRulesEngine {

    public boolean evaluateConditions(User user,
                                      Object resource,
                                      String permission) {
        // Get conditional permissions for user's roles
        List<ConditionalPermission> rules = getConditionalPermissions(user.getRoles(), permission);

        if (rules.isEmpty()) {
            return true; // No conditions, standard permission check applies
        }

        // ALL conditions must pass
        return rules.stream().allMatch(rule ->
            evaluateCondition(user, resource, rule.getConditions())
        );
    }

    private boolean evaluateCondition(User user, Object resource, Condition condition) {
        Object actualValue = getAttributeValue(resource, condition.getAttribute());
        Object expectedValue = resolveValue(user, condition.getValue());

        return switch (condition.getOperator()) {
            case EQUALS -> Objects.equals(actualValue, expectedValue);
            case LESS_THAN -> compareNumeric(actualValue, expectedValue) < 0;
            case GREATER_THAN -> compareNumeric(actualValue, expectedValue) > 0;
            case IN -> ((Collection<?>) expectedValue).contains(actualValue);
            case BETWEEN -> isBetween(actualValue, (Object[]) expectedValue);
            default -> false;
        };
    }

    private Object getAttributeValue(Object resource, String attribute) {
        // Use reflection or Spring Expression Language
        if (attribute.startsWith("@user.")) {
            // User attribute reference
            return getUserAttribute(attribute.substring(6));
        }

        // Resource attribute
        String[] parts = attribute.split("\\.");
        Object current = resource;
        for (String part : parts) {
            current = getProperty(current, part);
        }
        return current;
    }
}
```

#### 3.3 Integrate with Permission Evaluator

```java
// Update ResourcePermissionEvaluator
@Component
public class ResourcePermissionEvaluator implements PermissionEvaluator {

    @Autowired private RbacRulesEngine rulesEngine;

    @Override
    public boolean hasPermission(Authentication authentication,
                                Object targetDomainObject,
                                Object permission) {
        // ... existing checks

        // NEW: Evaluate ABAC conditions
        User user = securityService.getCurrentUser();
        return rulesEngine.evaluateConditions(user, targetDomainObject, permission.toString());
    }
}
```

---

### **Phase 4: Field-Level Security**

#### 4.1 Add Field-Level Annotations

```java
// File: Property.java
@Entity
public class Property {

    @Id
    private Long id;

    // Public fields - anyone with property:read can see
    @JsonView(Views.Public.class)
    private String name;

    @JsonView(Views.Public.class)
    private String address;

    // Financial fields - only roles with financial permissions
    @JsonView(Views.Financial.class)
    @FieldPermission("financial:read")
    private BigDecimal purchasePrice;

    @JsonView(Views.Financial.class)
    @FieldPermission("financial:read")
    private BigDecimal currentValue;

    // Management fields - only property managers
    @JsonView(Views.Management.class)
    @FieldPermission("property:manage")
    private String privateNotes;

    @JsonView(Views.Management.class)
    @FieldPermission("property:manage")
    private BigDecimal targetRent;
}

// Views definition
public class Views {
    public interface Public {}
    public interface Financial extends Public {}
    public interface Management extends Public {}
    public interface Full extends Financial, Management {}
}
```

#### 4.2 Dynamic View Selection

```java
// File: PropertyController.java
@RestController
public class PropertyController {

    @GetMapping("/properties/{id}")
    public ResponseEntity<Property> getProperty(@PathVariable Long id) {
        Property property = propertyService.findById(id);
        User user = securityService.getCurrentUser();

        // Determine view based on user permissions
        Class<?> view = determineView(user);

        return ResponseEntity.ok()
            .body(new JsonViewWrapper<>(property, view));
    }

    private Class<?> determineView(User user) {
        if (hasPermission(user, "financial:read") && hasPermission(user, "property:manage")) {
            return Views.Full.class;
        } else if (hasPermission(user, "financial:read")) {
            return Views.Financial.class;
        } else if (hasPermission(user, "property:manage")) {
            return Views.Management.class;
        }
        return Views.Public.class;
    }
}
```

---

### **Phase 5: Time-Based & Delegated Permissions**

#### 5.1 Add Temporal Permissions Table

```sql
CREATE TABLE user_temporal_permissions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role VARCHAR(100),
    permission VARCHAR(255),
    resource_type VARCHAR(100),
    resource_id BIGINT,

    granted_by BIGINT NOT NULL,
    granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,

    reason VARCHAR(500),
    is_active BOOLEAN DEFAULT true,
    revoked_at TIMESTAMP NULL,
    revoked_by BIGINT NULL,

    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (granted_by) REFERENCES users(id),
    FOREIGN KEY (revoked_by) REFERENCES users(id),

    INDEX idx_user_active (user_id, is_active, expires_at),
    INDEX idx_expiry (expires_at)
);
```

#### 5.2 Temporal Permission Service

```java
// File: TemporalPermissionService.java
@Service
public class TemporalPermissionService {

    /**
     * Grant temporary permission to user
     */
    public void grantTemporaryPermission(
        Long userId,
        String permission,
        Duration duration,
        String reason) {

        User grantedBy = securityService.getCurrentUser();

        // Verify granter has authority to delegate
        if (!canDelegatePermission(grantedBy, permission)) {
            throw new PermissionDeniedException(
                "You cannot delegate permission: " + permission);
        }

        TemporalPermission temp = new TemporalPermission();
        temp.setUserId(userId);
        temp.setPermission(permission);
        temp.setGrantedBy(grantedBy.getId());
        temp.setGrantedAt(LocalDateTime.now());
        temp.setExpiresAt(LocalDateTime.now().plus(duration));
        temp.setReason(reason);
        temp.setActive(true);

        temporalPermissionRepository.save(temp);

        // Notify user
        notificationService.notifyTemporaryAccess(userId, permission, duration);
    }

    /**
     * Get active temporal permissions for user
     */
    public Set<String> getActiveTemporalPermissions(Long userId) {
        LocalDateTime now = LocalDateTime.now();

        return temporalPermissionRepository
            .findByUserIdAndIsActiveTrueAndExpiresAtAfter(userId, now)
            .stream()
            .map(TemporalPermission::getPermission)
            .collect(Collectors.toSet());
    }

    /**
     * Auto-expire old permissions (scheduled task)
     */
    @Scheduled(fixedRate = 60000) // Every minute
    public void expireOldPermissions() {
        LocalDateTime now = LocalDateTime.now();
        List<TemporalPermission> expired = temporalPermissionRepository
            .findByIsActiveTrueAndExpiresAtBefore(now);

        expired.forEach(perm -> {
            perm.setActive(false);
            // Notify user that access has expired
            notificationService.notifyAccessExpired(perm.getUserId(), perm.getPermission());
        });

        temporalPermissionRepository.saveAll(expired);
    }
}
```

#### 5.3 Update Authentication Filter

```java
// File: JwtAuthenticationFilter.java
private Collection<GrantedAuthority> loadRbacAuthorities(Set<String> roles, String username) {
    Set<GrantedAuthority> authorities = new HashSet<>();

    // ... existing role-based permissions

    // NEW: Add temporal permissions
    User user = userService.findByUsername(username);
    Set<String> temporalPerms = temporalPermissionService.getActiveTemporalPermissions(user.getId());
    temporalPerms.forEach(perm ->
        authorities.add(new SimpleGrantedAuthority(perm))
    );

    return authorities;
}
```

---

### **Phase 6: Permission Delegation**

#### 6.1 Add Delegation Rules to RBAC

```yaml
# File: rbac.yml
roles:
  PROPERTY_MANAGER:
    # ... existing config
    delegationRules:
      # Can delegate to assistant
      canDelegate:
        - "property:update"
        - "maintenance:assign"
        - "showing:schedule"
      canDelegateTo:
        - "ASSISTANT_PROPERTY_MANAGER"
        - "LEASING_COORDINATOR"
      maxDelegationDuration: "7d"
      requireApproval: false

  ORGANIZATION_OWNER:
    delegationRules:
      # Can delegate almost everything
      canDelegate:
        - "property:*"
        - "lease:*"
        - "financial:*"
      canDelegateTo:
        - "ORGANIZATION_ADMIN"
        - "PROPERTY_MANAGER"
      maxDelegationDuration: "30d"
      requireApproval: false
      requireReason: true
```

#### 6.2 Delegation API

```java
// File: DelegationController.java
@RestController
@RequestMapping("/api/v1/delegation")
public class DelegationController {

    @PostMapping("/delegate")
    public DelegationResponse delegatePermission(
        @RequestBody DelegationRequest request) {

        User delegator = securityService.getCurrentUser();

        // Validate delegation request
        validationService.validateDelegation(
            delegator,
            request.getToUserId(),
            request.getPermission(),
            request.getDuration()
        );

        // Create temporary permission
        temporalPermissionService.grantTemporaryPermission(
            request.getToUserId(),
            request.getPermission(),
            request.getDuration(),
            "Delegated by " + delegator.getUsername() + ": " + request.getReason()
        );

        // Audit log
        auditService.logDelegation(delegator, request);

        return DelegationResponse.success("Permission delegated successfully");
    }

    @GetMapping("/my-delegations")
    public List<DelegationInfo> getMyDelegations() {
        User user = securityService.getCurrentUser();

        // Permissions I've delegated to others
        List<TemporalPermission> delegated =
            temporalPermissionRepository.findByGrantedByAndIsActiveTrue(user.getId());

        // Permissions delegated to me
        List<TemporalPermission> received =
            temporalPermissionRepository.findByUserIdAndIsActiveTrue(user.getId());

        return buildDelegationInfoList(delegated, received);
    }

    @DeleteMapping("/revoke/{delegationId}")
    public void revokeDelegation(@PathVariable Long delegationId) {
        temporalPermissionService.revokeDelegation(delegationId);
    }
}
```

---

### **Phase 7: Dynamic Permission Context**

#### 7.1 Add Context Resolver

```java
// File: PermissionContext.java
@Component
public class PermissionContext {

    private static final ThreadLocal<Map<String, Object>> context =
        ThreadLocal.withInitial(HashMap::new);

    public static void set(String key, Object value) {
        context.get().put(key, value);
    }

    public static Object get(String key) {
        return context.get().get(key);
    }

    public static void clear() {
        context.remove();
    }

    // Pre-populate context with request info
    public static void initializeFromRequest(HttpServletRequest request, User user) {
        clear();

        // Add common context attributes
        set("user", user);
        set("userId", user.getId());
        set("organizationId", user.getOrganizationId());
        set("roles", user.getRoles());
        set("timestamp", LocalDateTime.now());
        set("requestPath", request.getRequestURI());
        set("requestMethod", request.getMethod());
        set("clientIp", getClientIp(request));

        // Business hours check
        LocalTime now = LocalTime.now();
        boolean isBusinessHours = now.isAfter(LocalTime.of(9, 0)) &&
                                 now.isBefore(LocalTime.of(18, 0));
        set("isBusinessHours", isBusinessHours);

        // Day of week
        set("dayOfWeek", LocalDate.now().getDayOfWeek());
    }
}
```

#### 7.2 Add Context Filter

```java
// File: PermissionContextFilter.java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class PermissionContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {
        try {
            // Initialize context
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                User user = securityService.getCurrentUser();
                PermissionContext.initializeFromRequest(request, user);
            }

            filterChain.doFilter(request, response);
        } finally {
            // Always clean up
            PermissionContext.clear();
        }
    }
}
```

#### 7.3 Use Context in Rules

```yaml
# File: rbac.yml
roles:
  MAINTENANCE_TECH:
    conditionalPermissions:
      # Can only update tickets during business hours
      - permission: "maintenance:update"
        conditions:
          - attribute: "@context.isBusinessHours"
            operator: "EQUALS"
            value: true

      # Can only complete tickets assigned to them
      - permission: "maintenance:complete"
        conditions:
          - attribute: "ticket.assignedTo"
            operator: "EQUALS"
            value: "@context.userId"
```

---

## 📊 Implementation Priority Matrix

| Phase                       | Priority    | Complexity | Impact | Effort    | Dependencies |
| --------------------------- | ----------- | ---------- | ------ | --------- | ------------ |
| **Phase 1: Multi-Role**     | 🔴 Critical | Medium     | High   | 2-3 weeks | None         |
| **Phase 2: Resource-Level** | 🔴 Critical | Medium     | High   | 2 weeks   | Phase 1      |
| **Phase 3: ABAC**           | 🟡 High     | High       | High   | 3-4 weeks | Phase 1, 2   |
| **Phase 4: Field-Level**    | 🟡 High     | Low        | Medium | 1 week    | None         |
| **Phase 5: Temporal**       | 🟢 Medium   | Medium     | Medium | 2 weeks   | Phase 1      |
| **Phase 6: Delegation**     | 🟢 Medium   | Medium     | Low    | 1-2 weeks | Phase 5      |
| **Phase 7: Context**        | 🔵 Low      | Low        | Low    | 1 week    | Phase 3      |

---

## 🎯 Quick Wins (Implement First)

### 1. **Multi-Role Support** (Phase 1)

- **Why**: Solves immediate limitation of users wearing multiple hats
- **Impact**: Users can be both PROPERTY_MANAGER and LEASING_AGENT
- **Effort**: 2-3 weeks
- **Breaking Changes**: Requires DB migration

### 2. **Resource-Level Permissions** (Phase 2)

- **Why**: Prevents users from accessing resources they shouldn't
- **Impact**: Fine-grained access control per property/lease
- **Effort**: 2 weeks
- **Breaking Changes**: None (additive)

### 3. **Field-Level Security** (Phase 4)

- **Why**: Easy to implement, immediate value
- **Impact**: Different roles see different data fields
- **Effort**: 1 week
- **Breaking Changes**: None

---

## 🔧 Additional Enhancements

### **8.1 Permission Caching Strategy**

```java
// File: CachedPermissionService.java
@Service
public class CachedPermissionService {

    @Cacheable(value = "userPermissions", key = "#userId")
    public Set<String> getUserPermissions(Long userId) {
        // Load from database
        User user = userRepository.findById(userId);
        Set<String> permissions = new HashSet<>();

        // Role-based permissions
        user.getRoles().forEach(role ->
            permissions.addAll(rbacService.getPermissionsForRole(role.name()))
        );

        // Temporal permissions
        permissions.addAll(temporalPermissionService.getActiveTemporalPermissions(userId));

        return permissions;
    }

    @CacheEvict(value = "userPermissions", key = "#userId")
    public void invalidateUserPermissions(Long userId) {
        // Called when user roles change or temporal permissions expire
    }
}
```

### **8.2 Permission Audit Trail**

```sql
CREATE TABLE permission_audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    action VARCHAR(50) NOT NULL, -- CHECK, GRANT, DENY, REVOKE
    permission VARCHAR(255) NOT NULL,
    resource_type VARCHAR(100),
    resource_id BIGINT,
    result VARCHAR(20), -- ALLOWED, DENIED
    reason VARCHAR(500),
    context JSON, -- Store evaluation context
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_user_time (user_id, created_at),
    INDEX idx_permission (permission),
    INDEX idx_result (result)
);
```

### **8.3 Permission Analysis API**

```java
// File: PermissionAnalysisController.java
@RestController
@RequestMapping("/api/v1/permissions")
public class PermissionAnalysisController {

    // "What can this user do?"
    @GetMapping("/user/{userId}/can-do")
    public List<String> whatCanUserDo(@PathVariable Long userId) {
        return permissionService.getAllPermissions(userId);
    }

    // "Who can perform this action?"
    @GetMapping("/who-can/{permission}")
    public List<UserSummary> whoCanDoThis(@PathVariable String permission) {
        return userService.findUsersWithPermission(permission);
    }

    // "Why can't this user do this?"
    @GetMapping("/user/{userId}/why-not/{permission}")
    public PermissionDenialReason whyCantUserDo(
        @PathVariable Long userId,
        @PathVariable String permission) {

        return permissionAnalysisService.explainDenial(userId, permission);
    }

    // "What permissions does this role have?"
    @GetMapping("/role/{role}/permissions")
    public Set<String> getRolePermissions(@PathVariable String role) {
        return rbacService.getPermissionsForRole(role);
    }
}
```

---

## 🚨 Security Considerations

### **1. Principle of Least Privilege**

```yaml
# Always grant minimum required permissions
LEASING_AGENT:
  permissions:
    - "property:read" # ✅ Can view properties
    - "showing:schedule" # ✅ Can schedule showings
    # ❌ NOT "property:update" - don't need to modify properties
    # ❌ NOT "property:delete" - definitely don't need deletion
```

### **2. Separation of Duties**

```yaml
# Prevent conflicts of interest
PROPERTY_ACCOUNTANT:
  permissions:
    - "payment:process"
    - "invoice:create"
  explicitDenials:
    - "payment:approve" # ❌ Can't approve own transactions

# Require dual approval for sensitive operations
APPROVAL_WORKFLOW:
  expense:approve:
    - condition: "amount > 10000"
      requires:
        - role: "PROPERTY_MANAGER"
        - role: "ORGANIZATION_OWNER"
```

### **3. Audit Everything**

```java
@Aspect
@Component
public class PermissionAuditAspect {

    @AfterReturning(
        pointcut = "@annotation(PreAuthorize)",
        returning = "result"
    )
    public void auditPermissionCheck(JoinPoint joinPoint, Object result) {
        String method = joinPoint.getSignature().toShortString();
        User user = securityService.getCurrentUser();

        auditService.log(new PermissionAuditEvent(
            user.getId(),
            method,
            "ALLOWED",
            PermissionContext.get("context")
        ));
    }
}
```

---

## 📈 Monitoring & Analytics

### **Dashboard Metrics**

```java
// Track permission usage
- Most frequently checked permissions
- Most frequently denied permissions
- Users with most permission changes
- Temporal permissions about to expire
- Delegation chains (who delegated to whom)
- Permission escalation attempts
```

### **Alerting Rules**

```yaml
alerts:
  - name: "Unusual Permission Activity"
    condition: "user.permissionChecks > 1000 in 1 hour"
    action: "notify_security_team"

  - name: "Repeated Permission Denials"
    condition: "user.permissionDenials > 50 for same_permission"
    action: "create_ticket"

  - name: "Expired Temporal Permission Not Revoked"
    condition: "temporalPermission.expiresAt < now() and active = true"
    action: "auto_revoke and notify"
```

---

## 🎓 Best Practices

### **1. Role Naming Convention**

```
[SCOPE]_[FUNCTION]_[LEVEL]

Examples:
- PLATFORM_OVERSIGHT (platform scope, oversight function, top level)
- ORGANIZATION_ADMIN (organization scope, admin function)
- PROPERTY_MANAGER (property scope, manager function)
- TEAM_COORDINATOR (team scope, coordinator function)
```

### **2. Permission Naming Convention**

```
[resource]:[action]:[modifier?]

Examples:
- property:read
- property:update
- property:delete
- financial:read:sensitive
- report:export:all_organizations
```

### **3. Testing Strategy**

```java
@Test
public void testPropertyManager_CanUpdateAssignedProperties() {
    User manager = createPropertyManager();
    Property assigned = createPropertyAssignedTo(manager);
    Property unassigned = createPropertyNotAssignedTo(manager);

    // Should be able to update assigned property
    assertTrue(permissionEvaluator.hasPermission(
        manager.getAuthentication(), assigned, "property:update"));

    // Should NOT be able to update unassigned property
    assertFalse(permissionEvaluator.hasPermission(
        manager.getAuthentication(), unassigned, "property:update"));
}
```

---

## 🔄 Migration Strategy

### **Week 1-2: Multi-Role Foundation**

1. Create `user_roles` table
2. Migrate existing single role data
3. Update JWT generation
4. Update authentication filter
5. Test thoroughly

### **Week 3-4: Resource-Level Permissions**

1. Implement `PermissionEvaluator`
2. Add ownership/assignment checks
3. Update controllers to use `hasPermission()`
4. Test with real scenarios

### **Week 5-6: Field-Level Security**

1. Add `@JsonView` annotations
2. Implement dynamic view selection
3. Update DTOs
4. Test data visibility

### **Week 7-10: ABAC & Temporal**

1. Design rules engine
2. Implement condition evaluation
3. Add temporal permissions
4. Build delegation system

---

## 📚 Additional Resources

### **Spring Security Documentation**

- [Method Security](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html)
- [Expression-Based Access Control](https://docs.spring.io/spring-security/reference/servlet/authorization/expression-based.html)

### **RBAC Standards**

- [NIST RBAC Model](https://csrc.nist.gov/projects/role-based-access-control)
- [OWASP Access Control](https://owasp.org/www-community/Access_Control)

### **Similar Implementations**

- [Casbin](https://casbin.org/) - Authorization library with RBAC, ABAC support
- [Apache Shiro](https://shiro.apache.org/) - Java security framework
- [Keycloak](https://www.keycloak.org/) - Identity and access management

---

## 🎯 Success Criteria

### **Phase 1 Complete When:**

- ✅ Users can have multiple roles
- ✅ JWT contains all roles
- ✅ Permissions aggregate from all roles
- ✅ Existing single-role users migrated
- ✅ All tests passing

### **Phase 2 Complete When:**

- ✅ Resource ownership checked automatically
- ✅ `hasPermission()` works in `@PreAuthorize`
- ✅ Property managers only see their properties
- ✅ Audit logs show resource-level checks

### **Phase 3 Complete When:**

- ✅ Conditional rules defined in YAML
- ✅ Rules engine evaluates attributes
- ✅ Context-aware permissions work
- ✅ Business rules enforced automatically

---

## 🤝 Need Help?

This is a comprehensive enhancement plan. Start with **Phase 1** (Multi-Role) and **Phase 2** (Resource-Level) as they provide the most immediate value.

---

## ✅ IMPLEMENTATION STATUS UPDATE (Completed)

### Centralized RBAC Architecture — DONE

The RBAC engine has been **fully centralized in auth-service**. All RBAC code was removed from propertize (55+ files cleaned). The architecture is now:

```
┌─────────────────────┐     ┌──────────────────────────────────┐
│    API Gateway       │     │         auth-service              │
│ (Edge enforcement)   │────▶│  ┌──────────────────────────┐    │
│ Local rbac.yml copy  │     │  │  Centralized RBAC Engine  │   │
│ for fast resolution  │     │  │  ─────────────────────    │   │
└─────────────────────┘     │  │  PolicyEngine (ABAC)      │   │
                            │  │  RbacService              │   │
┌─────────────────────┐     │  │  AuthorizationService     │   │
│    propertize        │────▶│  │  RbacConfigService        │   │
│ (JWT role-only auth) │ REST│  │  OwnershipEvaluator       │   │
│ No RBAC code        │ API │  │  CacheConfig              │   │
└─────────────────────┘     │  └──────────────────────────┘    │
                            │                                   │
┌─────────────────────┐     │  REST API: /api/v1/auth/          │
│  employee-service    │────▶│  • POST /authorize                │
│ (No RBAC refs)       │     │  • GET  /permissions/{role}       │
└─────────────────────┘     │  • POST /permissions/resolve      │
                            │  • POST /permissions/check        │
                            │  • GET  /roles, /roles/{role}     │
                            │  • GET  /rbac/config, /endpoints  │
                            │  • POST /cache/invalidate         │
                            └──────────────────────────────────┘
```

### Auth-Service RBAC Engine Components

| Component                          | File                                                      | Status |
| ---------------------------------- | --------------------------------------------------------- | ------ |
| PolicyEngine interface             | `rbac/engine/PolicyEngine.java`                           | ✅     |
| DefaultPolicyEngine (ABAC)         | `rbac/engine/DefaultPolicyEngine.java`                    | ✅     |
| PolicyContext (immutable request)  | `rbac/engine/PolicyContext.java`                          | ✅     |
| PolicyDecision (immutable result)  | `rbac/engine/PolicyDecision.java`                         | ✅     |
| Action enum (50+ actions)          | `rbac/engine/Action.java`                                 | ✅     |
| Resource enum (30+ resources)      | `rbac/engine/Resource.java`                               | ✅     |
| ConditionEvaluator interface       | `rbac/engine/ConditionEvaluator.java`                     | ✅     |
| OwnershipConditionEvaluator        | `rbac/engine/evaluators/OwnershipConditionEvaluator.java` | ✅     |
| RbacService (permission resolver)  | `service/RbacService.java`                                | ✅     |
| AuthorizationService (entry point) | `service/AuthorizationService.java`                       | ✅     |
| RbacConfigService                  | `service/RbacConfigService.java`                          | ✅     |
| RbacController (10 endpoints)      | `controller/RbacController.java`                          | ✅     |
| RbacConfig (YAML binding)          | `config/RbacConfig.java`                                  | ✅     |
| CacheConfig                        | `config/CacheConfig.java`                                 | ✅     |

### Unit Tests — 205 tests, ALL PASSING

| Test Class                      | Tests | Coverage Area                                                            |
| ------------------------------- | ----- | ------------------------------------------------------------------------ |
| RbacServiceTest                 | 37    | Permission resolution, inheritance, wildcard, canonical, SecurityContext |
| AuthorizationServiceTest        | 14    | authorize(), bypass, fallback, batch, cache                              |
| DefaultPolicyEngineTest         | 19    | evaluate(), ABAC, hasPermission, batch, composite                        |
| OwnershipConditionEvaluatorTest | 15    | Owner match, org match, admin bypass, passthrough                        |
| PolicyContextTest               | 22    | Builder, roles, attributes, factories, display name                      |
| PolicyDecisionTest              | 16    | Factories, admin bypass detection, summary, audit log                    |
| RbacConfigServiceTest           | 14    | Config map, role details, scope filtering                                |
| RbacControllerTest              | 18    | All 10 REST endpoints                                                    |
| ActionTest                      | 25    | Key lookup, existence, scope classification                              |
| ResourceTest                    | 24    | Key lookup, existence, categories                                        |
| TokenBlacklistServiceTest       | 21    | (pre-existing) Token blacklisting                                        |

---

## 🎯 REMAINING GRANULARITY IMPROVEMENTS

The following improvements can be made to make the RBAC system even more granular and dynamic. They are ordered by impact and feasibility:

### 🔴 High Priority — Immediate Value

#### 1. Field-Level Permissions

**Current**: Users who can read a resource see ALL fields.
**Improvement**: Control which fields are visible per role.

```yaml
# In rbac.yml
field_level_permissions:
  tenant:
    PROPERTY_MANAGER:
      visible: [name, email, phone, lease_info, payment_status]
      hidden: [ssn, bank_account, credit_score]
    LEASING_AGENT:
      visible: [name, email, phone, rental_application]
      hidden: [ssn, bank_account, payment_history]
    TENANT_SELF:
      visible: [name, email, phone, ssn, bank_account] # Can see own data
```

**Implementation**:

- Add `FieldLevelPermissionService` to auth-service
- Add `/api/v1/auth/fields/{resource}/{role}` endpoint
- Create `@FilterFields` annotation in propertize for auto-filtering response DTOs

#### 2. Time-Based Access Control

**Current**: Permissions are always active once assigned.
**Improvement**: Restrict access by time window.

```yaml
# In rbac.yml
time_restrictions:
  MAINTENANCE_COORDINATOR:
    active_hours: "08:00-18:00" # Business hours only
    active_days: ["MON", "TUE", "WED", "THU", "FRI"]
    timezone: "America/New_York"
  EMERGENCY_RESPONDER:
    active_hours: "00:00-23:59" # Always available
```

**Implementation**:

- Add `TimeBasedConditionEvaluator` implementing `ConditionEvaluator`
- Auto-evaluate time conditions in `DefaultPolicyEngine.evaluate()`

#### 3. Data Scope Constraints (Row-Level Security)

**Current**: Ownership check exists but limited to direct owner or same org.
**Improvement**: Portfolio-scoped, team-scoped, and assignment-based access.

```yaml
# In rbac.yml
data_scopes:
  PROPERTY_MANAGER:
    property: "assigned_portfolio" # Only properties in their portfolio
    tenant: "own_properties" # Tenants of their properties
    maintenance: "own_properties" # Maintenance for their properties
  REGIONAL_MANAGER:
    property: "own_region" # Properties in their region
    tenant: "own_region"
```

**Implementation**:

- Add `DataScopeConditionEvaluator` implementing `ConditionEvaluator`
- Add scope resolution endpoint: `POST /api/v1/auth/scope/resolve`
- Each service appends data-scope filters to DB queries

### 🟡 Medium Priority — Enhanced Control

#### 4. Conditional Permissions (Amount/Value Limits)

**Current**: Permission is binary — allowed or denied.
**Improvement**: Permitted with conditions (e.g., financial limits).

```yaml
# In rbac.yml
conditional_permissions:
  PROPERTY_MANAGER:
    payment:approve:
      max_amount: 5000
      requires_secondary_approval: false
    maintenance:approve:
      max_cost: 2500
  REGIONAL_MANAGER:
    payment:approve:
      max_amount: 25000
      requires_secondary_approval_above: 10000
```

**Implementation**:

- Add `ConditionalPermissionEvaluator` implementing `ConditionEvaluator`
- Pass `amount`, `cost` in ABAC attributes
- Return conditions in `PolicyDecision.conditionResults`

#### 5. Dynamic Role Composition at Runtime

**Current**: Roles are statically assigned in the database.
**Improvement**: Compose roles dynamically based on context.

**Implementation**:

- Add `DynamicRoleComposer` that checks `PolicyContext.attributes`
- Hook into `AuthorizationService.authorize()` before policy evaluation
- Define composition rules in `rbac.yml`

#### 6. Permission Delegation (Temporary Granting)

**Current**: Only admins can change roles. No temporary delegation.
**Improvement**: Users can temporarily delegate specific permissions.

```yaml
# In rbac.yml
delegation_rules:
  PROPERTY_MANAGER:
    can_delegate:
      - "maintenance:approve"
      - "tenant:read"
    max_duration_hours: 72
    requires_approval: false
  ORGANIZATION_ADMIN:
    can_delegate:
      - "property:*"
      - "tenant:*"
    max_duration_hours: 168
    requires_approval: true
```

**Implementation**:

- Add `PermissionDelegation` entity (delegator, delegatee, permissions, expiry)
- Add `DelegationService` with create/revoke/check
- Add `/api/v1/auth/delegations` CRUD endpoints
- Check delegations in `DefaultPolicyEngine.hasPermission()`

### 🟢 Future — Advanced Features

#### 7. Multi-Tenancy Isolation Enforcement

#### 8. Permission Audit Trail with Diff

#### 9. Custom Role Builder API (Runtime role creation)

#### 10. IP-Range / Geo-Location Based Access

---

## 🗺️ Implementation Roadmap (Updated)

| Phase   | Feature                                  | Effort   | Status           |
| ------- | ---------------------------------------- | -------- | ---------------- |
| ✅ Done | Centralized RBAC Engine in auth-service  | —        | **COMPLETE**     |
| ✅ Done | PolicyEngine with ABAC (ownership)       | —        | **COMPLETE**     |
| ✅ Done | 10 REST endpoints for cross-service auth | —        | **COMPLETE**     |
| ✅ Done | 205 unit tests, all passing              | —        | **COMPLETE**     |
| ✅ Done | Cache layer (ConcurrentMap)              | —        | **COMPLETE**     |
| Phase 1 | Field-Level Permissions                  | 2-3 days | 🔴 High Priority |
| Phase 1 | Time-Based Access Control                | 1-2 days | 🔴 High Priority |
| Phase 2 | Data Scope Constraints                   | 3-5 days | 🔴 High Priority |
| Phase 2 | Conditional Permissions                  | 2-3 days | 🟡 Medium        |
| Phase 3 | Dynamic Role Composition                 | 2-3 days | 🟡 Medium        |
| Phase 3 | Permission Delegation                    | 3-4 days | 🟡 Medium        |
| Phase 4 | Custom Role Builder API                  | 3-5 days | 🟢 Future        |
| Phase 4 | Permission Audit Trail                   | 2-3 days | 🟢 Future        |
| Phase 4 | IP/Geo-Location Access                   | 1-2 days | 🟢 Future        |
