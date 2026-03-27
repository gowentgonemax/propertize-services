# WageCraft Payroll System - Comprehensive Implementation Plan
**Date**: February 3, 2026  
**Status**: Analysis & Planning Phase

---

## 📋 **Table of Contents**
1. [Current State Analysis](#current-state-analysis)
2. [Critical Issues to Fix](#critical-issues-to-fix)
3. [Missing Entities & DTOs](#missing-entities--dtos)
4. [Missing API Endpoints](#missing-api-endpoints)
5. [Domain-Specific Refactoring Required](#domain-specific-refactoring-required)
6. [Implementation Phases](#implementation-phases)
7. [Architecture Recommendations](#architecture-recommendations)

---

## 🔍 **Current State Analysis**

### ✅ **What's Implemented**

#### **Entities**
- ✅ Client (model folder)
- ✅ Employee (model folder)
- ✅ User (model & entity folders)
- ✅ PayrollRun (model & entity folders)
- ✅ Paystub (model & entity folders)
- ✅ PaystubEarning, PaystubDeduction, PaystubTax (model & entity)
- ✅ PayrollAdjustment (model & entity)
- ✅ TimeEntry (model & entity)
- ✅ LeaveRequest (model & entity)
- ✅ BenefitPlan, BenefitEnrollment (model & entity)
- ✅ TaxConfiguration (model & entity)
- ✅ Deduction (model & entity)

#### **Controllers**
- ✅ AuthController (login, register)
- ✅ TokenController (refresh tokens)
- ✅ ClientController (basic CRUD)
- ✅ EmployeeController (basic CRUD)
- ✅ PayrollController (create, process, approve payroll)
- ✅ UserController (user management)

#### **Services**
- ✅ AuthService
- ✅ UserService
- ✅ ClientService
- ✅ EmployeeService
- ✅ PayrollService

#### **Repositories**
- ✅ ClientRepository
- ✅ EmployeeRepository
- ✅ PayrollRunRepository
- ✅ UserRepository

#### **DTOs** (Partial Implementation)
- ✅ Some payroll DTOs exist in `/dto/payroll/`
- ✅ Some auth DTOs exist in `/dto/auth/`

---

## ⚠️ **Critical Issues to Fix**

### **1. DUPLICATE ENTITY STRUCTURE**
**PROBLEM**: Both `model` and `entity` folders exist with duplicate classes
- `/model/Employee.java`
- `/entity/Employee.java` (if exists)

**IMPACT**: Confusion, potential conflicts, unclear architecture

**SOLUTION**: 
```
✅ DECISION: Use ONLY `/entity/` folder (aligning with Propertize architecture)
- Rename all `/model/*.java` → `/entity/*.java`
- Delete `/model/` folder
- Update all imports across codebase
```

---

### **2. MISSING DTO LAYER (REQUEST/RESPONSE SEPARATION)**
**PROBLEM**: Controllers are using entities directly instead of DTOs

**CURRENT BAD PRACTICE**:
```java
@PostMapping("/clients/{clientId}/employees")
public ResponseEntity<Employee> createEmployee(@RequestBody Employee employee)
```

**SHOULD BE**:
```java
@PostMapping("/clients/{clientId}/employees")
public ResponseEntity<EmployeeResponse> createEmployee(@RequestBody EmployeeRequest request)
```

**SOLUTION**: Create proper DTO structure
```
dto/
├── employee/
│   ├── request/
│   │   ├── EmployeeCreateRequest.java
│   │   ├── EmployeeUpdateRequest.java
│   │   └── EmployeeTerminateRequest.java
│   ├── response/
│   │   ├── EmployeeResponse.java
│   │   ├── EmployeeDetailResponse.java
│   │   └── EmployeeListResponse.java
│   └── dto/
│       ├── EmployeeCompensationDTO.java
│       └── EmployeeBenefitsDTO.java
```

---

### **3. MISSING REPOSITORIES**
**PROBLEM**: Only 4 repositories exist, but we have 15+ entities

**MISSING REPOSITORIES**:
- ❌ PaystubRepository
- ❌ TimeEntryRepository
- ❌ LeaveRequestRepository
- ❌ BenefitPlanRepository
- ❌ BenefitEnrollmentRepository
- ❌ TaxConfigurationRepository
- ❌ DeductionRepository
- ❌ PayrollAdjustmentRepository
- ❌ PaystubEarningRepository
- ❌ PaystubDeductionRepository
- ❌ PaystubTaxRepository

**SOLUTION**: Create all missing repositories

---

### **4. MISSING SERVICES & CONTROLLERS**
**PROBLEM**: Many entities exist but have no service/controller layer

**MISSING SERVICES**:
- ❌ TimeEntryService (for time tracking)
- ❌ LeaveRequestService (for leave management)
- ❌ PaystubService (for paystub generation/retrieval)
- ❌ BenefitService (for benefits administration)
- ❌ TaxService (for tax calculations)
- ❌ DeductionService (for deduction management)
- ❌ ReportService (for payroll reports)

**MISSING CONTROLLERS**:
- ❌ TimeEntryController
- ❌ LeaveRequestController
- ❌ PaystubController
- ❌ BenefitController
- ❌ TaxConfigurationController
- ❌ DeductionController
- ❌ ReportController

---

## 🏗️ **Missing Entities & DTOs**

### **Missing Core Entities**

#### **1. Department Entity** (Referenced but not implemented)
```java
@Entity
@Table(name = "departments")
public class Department extends BaseEntity {
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false, unique = true)
    private String code;
    
    private String description;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_department_id")
    private Department parentDepartment;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id")
    private Employee manager; // Reference to Employee microservice
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;
    
    @Enumerated(EnumType.STRING)
    private DepartmentStatusEnum status;
}
```

#### **2. Position Entity** (Referenced but not implemented)
```java
@Entity
@Table(name = "positions")
public class Position extends BaseEntity {
    @Column(nullable = false)
    private String title;
    
    @Column(nullable = false, unique = true)
    private String code;
    
    private String description;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;
    
    @Embedded
    private SalaryRange salaryRange;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;
    
    @Enumerated(EnumType.STRING)
    private PositionStatusEnum status;
}
```

#### **3. PayPeriod Entity**
```java
@Entity
@Table(name = "pay_periods")
public class PayPeriod extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;
    
    @Embedded
    private DatePeriod period;
    
    @Column(name = "pay_date", nullable = false)
    private LocalDate payDate;
    
    @Enumerated(EnumType.STRING)
    private PayPeriodStatusEnum status; // OPEN, PROCESSING, CLOSED
    
    private Boolean isLocked = false;
}
```

#### **4. Timesheet Entity** (Higher level than TimeEntry)
```java
@Entity
@Table(name = "timesheets")
public class Timesheet extends BaseEntity {
    @Column(name = "employee_id", nullable = false)
    private String employeeId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pay_period_id")
    private PayPeriod payPeriod;
    
    @Embedded
    private DatePeriod weekPeriod;
    
    @OneToMany(mappedBy = "timesheet", cascade = CascadeType.ALL)
    private List<TimeEntry> timeEntries = new ArrayList<>();
    
    @Column(precision = 10, scale = 2)
    private BigDecimal totalRegularHours;
    
    @Column(precision = 10, scale = 2)
    private BigDecimal totalOvertimeHours;
    
    @Enumerated(EnumType.STRING)
    private TimesheetStatusEnum status; // DRAFT, SUBMITTED, APPROVED, REJECTED
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;
}
```

#### **5. EmployeeBankAccount Entity** (For direct deposit)
```java
@Entity
@Table(name = "employee_bank_accounts")
public class EmployeeBankAccount extends BaseEntity {
    @Column(name = "employee_id", nullable = false)
    private String employeeId;
    
    @Embedded
    private BankingInfo bankingInfo;
    
    private Boolean isPrimary = false;
    
    @Column(precision = 5, scale = 2)
    private BigDecimal allocationPercentage; // For split deposits
    
    @Enumerated(EnumType.STRING)
    private BankAccountStatusEnum status;
}
```

#### **6. TaxWithholding Entity** (For W-4 information)
```java
@Entity
@Table(name = "tax_withholdings")
public class TaxWithholding extends BaseEntity {
    @Column(name = "employee_id", nullable = false)
    private String employeeId;
    
    @Enumerated(EnumType.STRING)
    private FilingStatusEnum filingStatus;
    
    private Integer numberOfDependents = 0;
    
    private BigDecimal additionalWithholding = BigDecimal.ZERO;
    
    private Boolean isExempt = false;
    
    @Column(name = "effective_date")
    private LocalDate effectiveDate;
    
    @Column(name = "w4_form_url")
    private String w4FormUrl; // Document storage
}
```

#### **7. PayrollTaxDeposit Entity** (For employer tax liabilities)
```java
@Entity
@Table(name = "payroll_tax_deposits")
public class PayrollTaxDeposit extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payroll_run_id")
    private PayrollRun payrollRun;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;
    
    @Enumerated(EnumType.STRING)
    private TaxTypeEnum taxType; // FEDERAL, STATE, SOCIAL_SECURITY, MEDICARE
    
    @Column(precision = 15, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "due_date")
    private LocalDate dueDate;
    
    @Column(name = "paid_date")
    private LocalDate paidDate;
    
    @Enumerated(EnumType.STRING)
    private TaxDepositStatusEnum status; // PENDING, PAID, OVERDUE
    
    private String referenceNumber;
}
```

#### **8. EmployeeDocument Entity**
```java
@Entity
@Table(name = "employee_documents")
public class EmployeeDocument extends BaseEntity {
    @Column(name = "employee_id", nullable = false)
    private String employeeId;
    
    @Enumerated(EnumType.STRING)
    private DocumentTypeEnum documentType; // W4, I9, OFFER_LETTER, etc.
    
    private String fileName;
    private String fileUrl;
    private String contentType;
    private Long fileSize;
    
    @Column(name = "uploaded_date")
    private LocalDateTime uploadedDate;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;
    
    @Column(name = "expiry_date")
    private LocalDate expiryDate; // For docs that expire
}
```

#### **9. PayrollHistory Entity** (Audit trail)
```java
@Entity
@Table(name = "payroll_history")
public class PayrollHistory extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payroll_run_id")
    private PayrollRun payrollRun;
    
    @Enumerated(EnumType.STRING)
    private PayrollActionEnum action; // CREATED, PROCESSED, APPROVED, VOIDED
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by")
    private User performedBy;
    
    @Column(name = "performed_at")
    private LocalDateTime performedAt;
    
    @Column(length = 1000)
    private String notes;
    
    @Column(columnDefinition = "TEXT")
    private String changeData; // JSON snapshot of changes
}
```

---

### **Missing Embeddable Value Objects**

#### **1. SalaryRange** (For Position)
```java
@Embeddable
@Getter
@Setter
public class SalaryRange implements Serializable {
    @Column(name = "min_salary", precision = 15, scale = 2)
    private BigDecimal minSalary;
    
    @Column(name = "max_salary", precision = 15, scale = 2)
    private BigDecimal maxSalary;
    
    @Column(name = "currency", length = 3)
    private String currency = "USD";
}
```

#### **2. TaxInfo** (Consolidate tax information)
```java
@Embeddable
@Getter
@Setter
public class TaxInfo implements Serializable {
    @Enumerated(EnumType.STRING)
    @Column(name = "filing_status", length = 30)
    private FilingStatusEnum filingStatus;
    
    @Column(name = "tax_exemptions")
    private Integer taxExemptions = 0;
    
    @Column(name = "additional_withholding", precision = 10, scale = 2)
    private BigDecimal additionalWithholding = BigDecimal.ZERO;
    
    @Column(name = "is_exempt")
    private Boolean isExempt = false;
}
```

---

### **Missing Enums**

```java
public enum DepartmentStatusEnum { ACTIVE, INACTIVE, ARCHIVED }
public enum PositionStatusEnum { ACTIVE, INACTIVE, ARCHIVED }
public enum PayPeriodStatusEnum { OPEN, PROCESSING, CLOSED }
public enum TimesheetStatusEnum { DRAFT, SUBMITTED, APPROVED, REJECTED }
public enum BankAccountStatusEnum { ACTIVE, INACTIVE, PENDING_VERIFICATION }
public enum TaxDepositStatusEnum { PENDING, PAID, OVERDUE }
public enum DocumentTypeEnum { 
    W4, W2, I9, OFFER_LETTER, CONTRACT, ID_PROOF, 
    CERTIFICATE, PERFORMANCE_REVIEW, OTHER 
}
public enum PayrollActionEnum { 
    CREATED, PROCESSED, APPROVED, VOIDED, RECALCULATED 
}
```

---

## 🚀 **Missing API Endpoints**

### **1. Time & Attendance APIs**

#### **TimeEntry Endpoints**
```
POST   /api/v1/employees/{employeeId}/time-entries
GET    /api/v1/employees/{employeeId}/time-entries
GET    /api/v1/employees/{employeeId}/time-entries/{id}
PUT    /api/v1/employees/{employeeId}/time-entries/{id}
DELETE /api/v1/employees/{employeeId}/time-entries/{id}
POST   /api/v1/employees/{employeeId}/time-entries/{id}/approve
POST   /api/v1/employees/{employeeId}/time-entries/{id}/reject
```

#### **Timesheet Endpoints**
```
POST   /api/v1/timesheets
GET    /api/v1/timesheets
GET    /api/v1/timesheets/{id}
PUT    /api/v1/timesheets/{id}
POST   /api/v1/timesheets/{id}/submit
POST   /api/v1/timesheets/{id}/approve
POST   /api/v1/timesheets/{id}/reject
GET    /api/v1/timesheets/period/{payPeriodId}
```

#### **Clock In/Out Endpoints**
```
POST   /api/v1/time-clock/clock-in
POST   /api/v1/time-clock/clock-out
GET    /api/v1/time-clock/current-status
GET    /api/v1/time-clock/today
```

---

### **2. Leave Management APIs**

```
POST   /api/v1/leave-requests
GET    /api/v1/leave-requests
GET    /api/v1/leave-requests/{id}
PUT    /api/v1/leave-requests/{id}
DELETE /api/v1/leave-requests/{id}
POST   /api/v1/leave-requests/{id}/approve
POST   /api/v1/leave-requests/{id}/reject
POST   /api/v1/leave-requests/{id}/cancel
GET    /api/v1/leave-requests/employee/{employeeId}
GET    /api/v1/leave-requests/pending-approval
GET    /api/v1/employees/{employeeId}/leave-balance
```

---

### **3. Benefits Administration APIs**

```
GET    /api/v1/benefit-plans
GET    /api/v1/benefit-plans/{id}
POST   /api/v1/benefit-plans
PUT    /api/v1/benefit-plans/{id}
DELETE /api/v1/benefit-plans/{id}

POST   /api/v1/employees/{employeeId}/benefits/enroll
GET    /api/v1/employees/{employeeId}/benefits
PUT    /api/v1/employees/{employeeId}/benefits/{enrollmentId}
DELETE /api/v1/employees/{employeeId}/benefits/{enrollmentId}
GET    /api/v1/benefit-enrollments/open-enrollment
```

---

### **4. Paystub & Payment APIs**

```
GET    /api/v1/paystubs
GET    /api/v1/paystubs/{id}
GET    /api/v1/paystubs/{id}/pdf
GET    /api/v1/employees/{employeeId}/paystubs
GET    /api/v1/employees/{employeeId}/paystubs/ytd-summary
POST   /api/v1/payroll/{payrollRunId}/generate-paystubs
```

---

### **5. Tax Configuration APIs**

```
GET    /api/v1/employees/{employeeId}/tax-configuration
POST   /api/v1/employees/{employeeId}/tax-configuration
PUT    /api/v1/employees/{employeeId}/tax-configuration/{id}
GET    /api/v1/clients/{clientId}/tax-configurations
POST   /api/v1/tax-deposits
GET    /api/v1/tax-deposits/due
```

---

### **6. Deduction Management APIs**

```
GET    /api/v1/deductions
POST   /api/v1/deductions
PUT    /api/v1/deductions/{id}
DELETE /api/v1/deductions/{id}
GET    /api/v1/employees/{employeeId}/deductions
POST   /api/v1/employees/{employeeId}/deductions/assign
```

---

### **7. Reporting APIs**

```
GET    /api/v1/reports/payroll-summary
GET    /api/v1/reports/payroll-detail
GET    /api/v1/reports/tax-liability
GET    /api/v1/reports/employee-earnings
GET    /api/v1/reports/quarter-end
POST   /api/v1/reports/export
GET    /api/v1/reports/compliance/941  // Quarterly tax form
GET    /api/v1/reports/compliance/w2   // Annual W-2 forms
```

---

### **8. Department & Position APIs**

```
GET    /api/v1/departments
POST   /api/v1/departments
GET    /api/v1/departments/{id}
PUT    /api/v1/departments/{id}
DELETE /api/v1/departments/{id}
GET    /api/v1/departments/{id}/employees

GET    /api/v1/positions
POST   /api/v1/positions
GET    /api/v1/positions/{id}
PUT    /api/v1/positions/{id}
DELETE /api/v1/positions/{id}
```

---

### **9. Dashboard & Analytics APIs**

```
GET    /api/v1/dashboard/overview
GET    /api/v1/dashboard/payroll-metrics
GET    /api/v1/dashboard/employee-metrics
GET    /api/v1/dashboard/upcoming-payrolls
GET    /api/v1/dashboard/pending-approvals
GET    /api/v1/analytics/labor-costs
GET    /api/v1/analytics/overtime-trends
```

---

## 🎯 **Domain-Specific Refactoring Required**

### **Phase 1: Entity Consolidation**
1. **Move all classes from `/model/` to `/entity/`**
2. **Delete `/model/` folder**
3. **Update all imports**
4. **Run tests to verify**

### **Phase 2: DTO Layer Implementation**
Create DTOs for all domains:

```
dto/
├── common/
│   ├── AddressDTO.java
│   ├── MoneyDTO.java
│   ├── DatePeriodDTO.java
│   └── PaginationDTO.java
├── auth/
│   ├── request/
│   │   ├── LoginRequest.java
│   │   └── RegisterRequest.java
│   └── response/
│       └── AuthResponse.java
├── client/
│   ├── request/
│   │   ├── ClientCreateRequest.java
│   │   └── ClientUpdateRequest.java
│   └── response/
│       ├── ClientResponse.java
│       └── ClientDetailResponse.java
├── employee/
│   ├── request/
│   │   ├── EmployeeCreateRequest.java
│   │   ├── EmployeeUpdateRequest.java
│   │   └── EmployeeTerminateRequest.java
│   └── response/
│       ├── EmployeeResponse.java
│       ├── EmployeeDetailResponse.java
│       └── EmployeeListResponse.java
├── payroll/
│   ├── request/
│   │   ├── PayrollRunCreateRequest.java
│   │   └── PayrollAdjustmentRequest.java
│   └── response/
│       ├── PayrollRunResponse.java
│       ├── PayrollSummaryResponse.java
│       └── PaystubResponse.java
├── timesheet/
│   ├── request/
│   │   ├── TimeEntryRequest.java
│   │   ├── TimesheetSubmitRequest.java
│   │   └── ClockInRequest.java
│   └── response/
│       ├── TimeEntryResponse.java
│       └── TimesheetResponse.java
├── leave/
│   ├── request/
│   │   ├── LeaveRequestCreate.java
│   │   └── LeaveApprovalRequest.java
│   └── response/
│       ├── LeaveRequestResponse.java
│       └── LeaveBalanceResponse.java
├── benefit/
│   ├── request/
│   │   ├── BenefitPlanRequest.java
│   │   └── BenefitEnrollmentRequest.java
│   └── response/
│       ├── BenefitPlanResponse.java
│       └── BenefitEnrollmentResponse.java
├── tax/
│   ├── request/
│   │   └── TaxConfigurationRequest.java
│   └── response/
│       ├── TaxConfigurationResponse.java
│       └── TaxLiabilityResponse.java
└── report/
    ├── request/
    │   └── ReportGenerationRequest.java
    └── response/
        ├── PayrollSummaryReport.java
        └── TaxReport.java
```

### **Phase 3: MapStruct Mappers**
```java
@Mapper(componentModel = "spring")
public interface EmployeeMapper {
    EmployeeResponse toResponse(Employee entity);
    List<EmployeeResponse> toResponseList(List<Employee> entities);
    Employee toEntity(EmployeeCreateRequest request);
    void updateEntityFromRequest(EmployeeUpdateRequest request, @MappingTarget Employee entity);
}
```

---

## 📅 **Implementation Phases**

### **Phase 1: Foundation (Week 1-2)**
**Priority**: CRITICAL

✅ **Tasks**:
1. Consolidate entity structure (model → entity)
2. Create missing repositories
3. Create missing enums
4. Create base DTO structure
5. Implement MapStruct mappers
6. Update existing controllers to use DTOs
7. Fix all import issues
8. Run and fix all tests

**Deliverables**:
- Clean entity structure
- All repositories created
- Base DTO framework
- All tests passing

---

### **Phase 2: Time & Attendance (Week 3-4)**
**Priority**: HIGH

✅ **Tasks**:
1. Create Department & Position entities
2. Create Timesheet & TimeEntry services
3. Implement TimeEntryController
4. Implement TimesheetController
5. Implement Clock In/Out functionality
6. Create time entry DTOs
7. Add validation rules
8. Write unit tests

**Deliverables**:
- Time tracking fully functional
- Employees can clock in/out
- Timesheets can be submitted & approved
- Integration with payroll

---

### **Phase 3: Leave Management (Week 4-5)**
**Priority**: HIGH

✅ **Tasks**:
1. Create LeaveRequestService
2. Create LeaveRequestController
3. Implement leave balance tracking
4. Create leave approval workflow
5. Add email notifications
6. Create leave DTOs
7. Write unit tests

**Deliverables**:
- Leave requests functional
- Approval workflow working
- Leave balance calculated correctly

---

### **Phase 4: Benefits Administration (Week 5-6)**
**Priority**: MEDIUM

✅ **Tasks**:
1. Create BenefitService
2. Create BenefitController
3. Implement benefit enrollment
4. Implement deduction calculation
5. Create benefit DTOs
6. Integration with payroll deductions
7. Write unit tests

**Deliverables**:
- Benefit plans manageable
- Employee enrollment working
- Deductions calculated correctly

---

### **Phase 5: Enhanced Payroll (Week 6-7)**
**Priority**: CRITICAL

✅ **Tasks**:
1. Create PaystubService
2. Create TaxService
3. Enhance PayrollService
4. Implement PDF generation
5. Create paystub DTOs
6. Add YTD calculations
7. Write unit tests

**Deliverables**:
- Paystubs generated correctly
- Tax calculations accurate
- PDF download working

---

### **Phase 6: Tax Management (Week 7-8)**
**Priority**: HIGH

✅ **Tasks**:
1. Create TaxConfiguration service & controller
2. Create PayrollTaxDeposit entity & service
3. Implement W-4 management
4. Create tax liability tracking
5. Add tax deposit reminders
6. Write unit tests

**Deliverables**:
- Tax withholding configured
- Tax deposits tracked
- Compliance reports available

---

### **Phase 7: Reporting & Analytics (Week 8-9)**
**Priority**: MEDIUM

✅ **Tasks**:
1. Create ReportService
2. Create ReportController
3. Implement payroll reports
4. Implement tax reports
5. Add export functionality (PDF, Excel, CSV)
6. Create dashboard APIs
7. Write unit tests

**Deliverables**:
- Comprehensive reports
- Export functionality
- Dashboard metrics

---

### **Phase 8: Employee Integration (Week 9-10)**
**Priority**: HIGH (If using Employee microservice)

✅ **Tasks**:
1. Create Feign client for Employee microservice
2. Implement employee sync
3. Handle employee data refresh
4. Add fallback mechanisms
5. Write integration tests

**Deliverables**:
- Employee microservice integrated
- Data sync working
- Error handling robust

---

## 🏛️ **Architecture Recommendations**

### **1. Follow Propertize Pattern**
```
entity/          - JPA entities (no builders)
dto/
  ├── domain/
  │   ├── request/
  │   ├── response/
  │   └── dto/
enums/           - All enums end with "Enum"
repository/      - Spring Data JPA repositories
service/         - Business logic
controller/      - REST endpoints (use DTOs)
mapper/          - MapStruct mappers
```

### **2. Use Domain-Driven Design**
- Group by business domain (payroll, time, leave, benefits)
- Each domain has its own service, controller, DTOs
- Use value objects (@Embeddable) for grouped fields

### **3. Use @Embeddable for Value Objects**
- Address
- Money
- DatePeriod
- BankingInfo
- ContactInfo
- TaxInfo
- SalaryRange

### **4. API Versioning**
```
/api/v1/employees
/api/v1/payroll
/api/v1/timesheets
```

### **5. Response Wrapper Pattern**
```java
{
  "success": true,
  "data": { ... },
  "message": "Operation successful",
  "timestamp": "2026-02-03T10:00:00Z"
}
```

### **6. Pagination**
```java
{
  "content": [ ... ],
  "page": 0,
  "size": 20,
  "totalElements": 150,
  "totalPages": 8
}
```

### **7. Error Handling**
```java
{
  "error": "VALIDATION_ERROR",
  "message": "Invalid employee data",
  "timestamp": "2026-02-03T10:00:00Z",
  "path": "/api/v1/employees",
  "details": {
    "email": "Email already exists"
  }
}
```

---

## 🎨 **Naming Conventions**

### **Entities**
- Singular noun: `Employee`, `Payroll`, `Timesheet`
- No "Entity" suffix
- Use `BaseEntity` for common fields

### **Enums**
- Always end with "Enum": `PayrollStatusEnum`
- Use `@Enumerated(EnumType.STRING)`

### **DTOs**
- Request: `EmployeeCreateRequest`, `PayrollUpdateRequest`
- Response: `EmployeeResponse`, `PayrollDetailResponse`
- Internal: `EmployeeCompensationDTO`

### **Services**
- Singular: `EmployeeService`, `PayrollService`
- Use `@Transactional` appropriately

### **Controllers**
- Plural path: `/employees`, `/payrolls`
- Use `@RestController` + `@RequestMapping`

---

## 🔐 **Security & Validation**

### **1. Method-Level Security**
```java
@PreAuthorize("hasRole('ADMIN')")
@PostMapping("/payroll/{id}/approve")
```

### **2. Input Validation**
```java
@Valid @RequestBody EmployeeCreateRequest request
```

### **3. Organization Isolation**
- Every entity should have `clientId` or `organizationId`
- Filter queries by organization

---

## 📊 **Success Metrics**

| Metric | Target |
|--------|--------|
| Code Coverage | > 80% |
| API Response Time | < 200ms (average) |
| Build Time | < 5 minutes |
| Zero Security Vulnerabilities | ✅ |
| All Tests Passing | ✅ |
| No Magic Strings | ✅ |
| No Duplicate Code | ✅ |
| Proper DTO Usage | 100% |

---

## 🎯 **Next Immediate Steps**

### **Week 1 - Days 1-3**
1. ✅ Run application and fix any startup errors
2. ✅ Move all files from `/model/` to `/entity/`
3. ✅ Create all missing repository interfaces
4. ✅ Create missing enum classes
5. ✅ Set up MapStruct dependency

### **Week 1 - Days 4-5**
6. ✅ Create DTO package structure
7. ✅ Create EmployeeMapper
8. ✅ Update EmployeeController to use DTOs
9. ✅ Write tests for EmployeeService

### **Week 2**
10. ✅ Implement TimeEntry functionality
11. ✅ Implement LeaveRequest functionality
12. ✅ Create Department & Position entities

---

## 📚 **Documentation Updates Needed**

1. ✅ Update FRONTEND_API_REFERENCE.md with new endpoints
2. ✅ Update FRONTEND_DATA_MODELS.md with new DTOs
3. ✅ Create API_CHANGELOG.md
4. ✅ Create ENTITY_RELATIONSHIP_DIAGRAM.md
5. ✅ Update README with setup instructions

---

## 🚨 **Blockers & Risks**

| Risk | Impact | Mitigation |
|------|--------|-----------|
| Duplicate entity structure | HIGH | Phase 1 priority fix |
| Missing employee microservice | MEDIUM | Use local employee entity temporarily |
| Tax calculation complexity | HIGH | Use external tax library |
| Paystub PDF generation | MEDIUM | Use iText or Jasper Reports |
| Integration with payment providers | MEDIUM | Phase 2 concern |

---

## ✅ **Definition of Done**

For each phase:
- [ ] All code written and reviewed
- [ ] All unit tests passing (> 80% coverage)
- [ ] Integration tests passing
- [ ] API documentation updated
- [ ] Frontend data models updated
- [ ] No security vulnerabilities
- [ ] Performance benchmarks met
- [ ] Code follows architecture guidelines
- [ ] Peer review completed

---

**END OF IMPLEMENTATION PLAN**

This document should be treated as a living document and updated as implementation progresses.
