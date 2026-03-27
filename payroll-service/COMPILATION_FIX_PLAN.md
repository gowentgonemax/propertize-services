# Wagecraft Compilation Fix Plan

## Date: February 5, 2026
## Status: IN PROGRESS

## Summary
The wagecraft project has multiple compilation errors that need to be fixed systematically. The main issues are:

1. Missing Lombok-generated methods (getters/setters)
2. Duplicate field definitions
3. Missing entity field mappings
4. Missing enum classes

## Errors Fixed So Far

### 1. WagecraftApplication.java
- ✅ Removed SessionAutoConfiguration exclusion (Spring Session not in dependencies)

### 2. BenefitEnrollment.java  
- ✅ Removed duplicate `terminationDate` field definition (was defined twice at lines 40 and 51)
- ✅ Added missing `employerContribution` field

### 3. pom.xml
- ✅ Added `lombok.version` property (1.18.30)

## Remaining Compilation Errors (100+ errors)

### Category 1: Missing Lombok Getters/Setters
**Issue**: Classes have `@Getter`/`@Setter`/`@Data` annotations but compilation fails to find the generated methods.

**Affected Files**:
- `User.java` - Missing UserDetails methods despite @Getter/@Setter
- `LoginRequest.java` - Has @Data but compilation can't find getEmail()/getPassword()
- `RegisterRequest.java` - Has @Data but compilation can't find getters
- `Client.java` - Missing getters for all fields
- `TimeEntry.java` - Missing getRegularHours()/getOvertimeHours()/getDoubleTimeHours()
- `PaystubEarning.java` - Missing getAmount()
- `PaystubDeduction.java` - Missing getAmount()
- `PaystubTax.java` - Missing getAmount()
- `PayrollTotals.java` - Missing getTotalGrossPay()/getTotalNetPay()/getTotalTaxes()/getTotalDeductions()
- `DatePeriod.java` - Missing getStartDate()/getEndDate()

**Solution**:
1. Run `mvn clean` to clear any stale compiled classes
2. Ensure Lombok annotation processor is running correctly
3. Verify IDE has Lombok plugin installed
4. As fallback, manually add getter/setter methods if Lombok processing fails

### Category 2: Missing Entity Fields
**Issue**: Methods referencing fields that don't exist in the entity classes.

**Affected Files**:
- `TimeEntry.java` - Missing `timesheet` field (needed for bidirectional relationship)
- `PaystubEarning.java` - Missing `paystub` field
- `PaystubDeduction.java` - Missing `paystub` field  
- `PaystubTax.java` - Missing `paystub` field

**Solution**: Add these missing fields with proper JPA annotations:
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "timesheet_id")
private TimesheetEntity timesheet;
```

### Category 3: Missing Enum Classes
**Issue**: Code references enum classes that don't exist.

**Missing Enums**:
- `PayFrequencyEnum` (referenced in CompensationService.java:338)

**Solution**: Create the missing enum class:
```java
package com.wagecraft.enums;

public enum PayFrequencyEnum {
    WEEKLY,
    BI_WEEKLY,
    SEMI_MONTHLY,
    MONTHLY
}
```

### Category 4: Missing @Slf4j Logger
**Issue**: Classes have `@Slf4j` annotation but `log` variable not found.

**Affected Files**:
- `LoggingAspect.java`
- `FeignErrorDecoder.java`
- `AuthController.java`
- `AuthService.java`
- `JwtTokenUtil.java`
- `UserService.java`

**Root Cause**: Lombok annotation processing not working properly.

**Solution**: Either fix Lombok processing or manually add:
```java
private static final Logger log = LoggerFactory.getLogger(ClassName.class);
```

### Category 5: Constructor/Builder Issues
**Issue**: Constructor or builder methods have wrong parameter counts.

**Affected Files**:
- `LoginResponse.java` - Constructor expects 4 parameters but being called with none

**Solution**: Review DTO classes and ensure constructors match usage, or use builder pattern.

## Step-by-Step Fix Plan

### Phase 1: Lombok Configuration (HIGH PRIORITY)
1. ✅ Add lombok.version property to pom.xml
2. Run `mvn clean install` to ensure Lombok processor runs
3. Verify target/classes contains generated methods
4. If fails, add explicit Lombok configuration to compiler plugin

### Phase 2: Entity Field Fixes (HIGH PRIORITY)
1. Add missing `timesheet` field to TimeEntry.java
2. Add missing `paystub` field to PaystubEarning.java
3. Add missing `paystub` field to PaystubDeduction.java
4. Add missing `paystub` field to PaystubTax.java
5. Fix BenefitEnrollment duplicate field issue

### Phase 3: Missing Enum Classes (MEDIUM PRIORITY)
1. Create PayFrequencyEnum.java
2. Verify all enum references are correct

### Phase 4: Manual Getters/Setters (If Lombok Fails) (MEDIUM PRIORITY)
1. For critical classes (User, LoginRequest, RegisterRequest), manually add getters/setters
2. Remove Lombok annotations from these classes
3. Test compilation

### Phase 5: Logger Fixes (LOW PRIORITY - Can Work Around)
1. If Lombok @Slf4j fails, manually add Logger fields
2. Replace `log` with explicit logger instances

### Phase 6: Integration Testing
1. Run full test suite
2. Fix any runtime errors
3. Verify application starts successfully

## Next Steps

1. **Immediate Action**: Run `mvn clean compile` and verify Lombok is processing annotations
2. **If Lombok works**: Most errors will auto-resolve
3. **If Lombok fails**: Execute Phase 4 (manual getters/setters) for critical classes
4. **Then**: Work through Phases 2-5 systematically

## Notes

- The duplicate `terminationDate` issue in BenefitEnrollment has been fixed
- Lombok version 1.18.30 has been added to pom.xml
- SessionAutoConfiguration exclusion has been removed from WagecraftApplication.java

## Estimated Time to Complete
- If Lombok works: 2-3 hours (mostly Phase 2 and 3)
- If Lombok fails: 6-8 hours (need to manually add all getters/setters)

## Dependencies on Other Services
- Wagecraft needs to integrate with:
  - propertize (property management)
  - employecraft (employee management)
  - api-gateway (routing)
  - service-registry (Eureka)

All these services need to be running and properly configured for full integration testing.
