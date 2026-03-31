# WageCraft Compilation Fix Guide
**Generated**: February 3, 2026

## Overview
The WageCraft project has 100+ compilation errors primarily caused by missing Lombok annotations on entity and DTO classes. This guide provides a systematic approach to fix all errors.

---

## Part 1: Add Lombok Annotations to Entities

### Priority 1: Core Entities

#### 1. Client.java
**Status**: Missing @Data or @Getter/@Setter  
**Location**: `/src/main/java/com/wagecraft/entity/Client.java`  
**Fix**:
```java
import lombok.Data;

@Entity
@Table(name = "clients")
@Data  // Add this annotation
public class Client extends BaseEntity {
    // ...existing fields...
}
```

#### 2. User.java
**Status**: Missing @Data, doesn't implement UserDetails properly  
**Location**: `/src/main/java/com/wagecraft/entity/User.java`  
**Fix**:
```java
import lombok.Data;

@Entity
@Table(name = "users")
@Data  // Add this annotation
public class User extends BaseEntity implements UserDetails {
    // ...existing fields...
    
    // Add UserDetails implementation
    @Override
    public String getPassword() {
        return this.password;
    }
    
    @Override
    public String getUsername() {
        return this.email;
    }
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    
    @Override
    public boolean isEnabled() {
        return this.enabled;
    }
}
```

#### 3. BenefitEnrollment.java
**Status**: Duplicate `terminationDate` field  
**Location**: `/src/main/java/com/wagecraft/entity/BenefitEnrollment.java`  
**Fix**: Remove one of the duplicate `terminationDate` declarations (line 51)

---

## Part 2: Fix Embedded Objects

### 1. DatePeriod.java
**Status**: Missing getStartDate()/getEndDate()  
**Location**: `/src/main/java/com/wagecraft/entity/embedded/DatePeriod.java`  
**Fix**:
```java
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DatePeriod {
    @Column(name = "start_date")
    private LocalDate startDate;
    
    @Column(name = "end_date")
    private LocalDate endDate;
}
```

### 2. PayrollTotals.java
**Status**: Missing getters for total fields  
**Location**: `/src/main/java/com/wagecraft/entity/embedded/PayrollTotals.java`  
**Fix**:
```java
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayrollTotals {
    @Column(name = "total_gross_pay", precision = 15, scale = 2)
    private BigDecimal totalGrossPay = BigDecimal.ZERO;
    
    @Column(name = "total_net_pay", precision = 15, scale = 2)
    private BigDecimal totalNetPay = BigDecimal.ZERO;
    
    @Column(name = "total_taxes", precision = 15, scale = 2)
    private BigDecimal totalTaxes = BigDecimal.ZERO;
    
    @Column(name = "total_deductions", precision = 15, scale = 2)
    private BigDecimal totalDeductions = BigDecimal.ZERO;
}
```

---

## Part 3: Fix DTOs and Request Classes

### 1. LoginRequest.java
**Location**: `/src/main/java/com/wagecraft/dto/auth/LoginRequest.java`  
**Fix**: Add @Data
```java
import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String password;
}
```

### 2. RegisterRequest.java
**Location**: `/src/main/java/com/wagecraft/dto/auth/RegisterRequest.java`  
**Fix**: Add @Data
```java
import lombok.Data;

@Data
public class RegisterRequest {
    private String email;
    private String password;
    private String firstName;
    private String lastName;
}
```

### 3. LoginResponse.java
**Location**: `/src/main/java/com/wagecraft/dto/auth/LoginResponse.java`  
**Fix**: Fix constructor to accept parameters
```java
@Data
@AllArgsConstructor  // Make sure this exists
@NoArgsConstructor
public class LoginResponse {
    private String token;
    private String refreshToken;
    private int expiresIn;
    private UserData user;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserData {
        private String email;
        private String firstName;
        private String lastName;
        private String role;
    }
}
```

---

## Part 4: Add @Slf4j Annotations

Add `import lombok.extern.slf4j.Slf4j;` and `@Slf4j` annotation to the following classes:

1. `/src/main/java/com/wagecraft/aspect/LoggingAspect.java`
2. `/src/main/java/com/wagecraft/client/FeignErrorDecoder.java`
3. `/src/main/java/com/wagecraft/controller/AuthController.java`
4. `/src/main/java/com/wagecraft/service/AuthService.java`
5. `/src/main/java/com/wagecraft/security/JwtTokenUtil.java`
6. `/src/main/java/com/wagecraft/service/UserService.java`

Example:
```java
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j  // Add this
public class AuthService {
    // ...
}
```

---

## Part 5: Fix Child Entity Relationships

### 1. TimeEntry.java
**Status**: Missing bidirectional relationship setters  
**Location**: `/src/main/java/com/wagecraft/entity/TimeEntry.java`  
**Fix**: Add @Data and ensure embedded TimeEntryHours has getters

### 2. PaystubEarning.java, PaystubDeduction.java, PaystubTax.java
**Status**: Missing getAmount() and setPaystub()  
**Fix**: Add @Data to each class

Example for PaystubEarning.java:
```java
import lombok.Data;

@Entity
@Table(name = "paystub_earnings")
@Data  // Add this
public class PaystubEarning extends BaseEntity {
    // ...existing fields...
}
```

---

## Part 6: Compilation Order

Fix in this order to minimize cascading errors:

1. **Embedded objects first** (DatePeriod, PayrollTotals, TimeEntryHours)
2. **Base entities** (Client, User)
3. **DTOs and Requests** (LoginRequest, RegisterRequest, LoginResponse)
4. **Service classes** (Add @Slf4j)
5. **Child entities** (TimeEntry, PaystubEarning, etc.)
6. **Run mvn clean compile** after each major section

---

## Verification Commands

```bash
# Clean and compile
mvn clean compile

# If successful, run tests
mvn test

# Full build
mvn clean package
```

---

## Expected Result

After all fixes:
- ✅ 0 compilation errors
- ✅ All Lombok annotations properly configured
- ✅ All entities have proper getters/setters
- ✅ All embedded objects accessible
- ✅ UserDetails implementation complete
- ✅ All logging working via @Slf4j

---

## Time Estimate

- Part 1 (Entities): 2 hours
- Part 2 (Embedded): 1 hour
- Part 3 (DTOs): 30 minutes
- Part 4 (@Slf4j): 30 minutes
- Part 5 (Relationships): 1 hour
- Part 6 (Compile & Fix): 2 hours

**Total: 7 hours**

---

## Notes

- All entities should extend BaseEntity which likely has id, createdAt, updatedAt
- Use @Data for simple POJOs and entities
- Use @Getter/@Setter separately only when you need to override specific methods
- Ensure lombok is in pom.xml dependencies and your IDE has Lombok plugin installed

