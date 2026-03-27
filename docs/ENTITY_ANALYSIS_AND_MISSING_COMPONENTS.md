# WageCraft - Entity Analysis & Missing Components
**Date**: February 3, 2026  
**Architect**: Senior Backend Domain Expert

---

## 📊 **Complete Entity Inventory**

### **Existing Entities** (Found in both `/model/` and `/entity/`)

| Entity | Location | Status | Issues | Priority |
|--------|----------|--------|--------|----------|
| Client | model + entity | ✅ Exists | Needs DTO layer | HIGH |
| Employee | model + entity | ✅ Exists | Should use Employee microservice | HIGH |
| User | model + entity | ✅ Exists | Needs role enhancement | MEDIUM |
| PayrollRun | model + entity | ✅ Exists | Needs DTO layer | HIGH |
| Paystub | model + entity | ✅ Exists | Missing PDF generation | HIGH |
| PaystubEarning | model + entity | ✅ Exists | OK | MEDIUM |
| PaystubDeduction | model + entity | ✅ Exists | OK | MEDIUM |
| PaystubTax | model + entity | ✅ Exists | OK | MEDIUM |
| PayrollAdjustment | model + entity | ✅ Exists | Needs service layer | HIGH |
| TimeEntry | model + entity | ✅ Exists | Needs controller | HIGH |
| LeaveRequest | model + entity | ✅ Exists | Needs controller | HIGH |
| BenefitPlan | model + entity | ✅ Exists | Needs service layer | MEDIUM |
| BenefitEnrollment | model + entity | ✅ Exists | Needs service layer | MEDIUM |
| TaxConfiguration | model + entity | ✅ Exists | Needs service layer | HIGH |
| Deduction | model + entity | ✅ Exists | Needs service layer | MEDIUM |

---

### **Missing Critical Entities**

#### **1. Department Entity** ⚠️ **CRITICAL**
**Why Missing**: Referenced in TimeEntry, Employee but not implemented  
**Impact**: Cannot organize employees, track costs by department

```java
@Entity
@Table(name = "departments", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"client_id", "code"}))
public class Department extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(nullable = false, length = 20)
    private String code; // e.g., "ENG", "HR", "FIN"
    
    @Column(length = 500)
    private String description;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_department_id")
    private Department parentDepartment;
    
    @Column(name = "manager_employee_id")
    private String managerEmployeeId; // Reference to Employee microservice
    
    @Column(name = "cost_center", length = 50)
    private String costCenter;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private DepartmentStatusEnum status = DepartmentStatusEnum.ACTIVE;
    
    // Soft delete
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    
    @Column(name = "deleted_by")
    private String deletedBy;
}
```

**Required Enum**:
```java
public enum DepartmentStatusEnum {
    ACTIVE,
    INACTIVE,
    ARCHIVED
}
```

---

#### **2. Position Entity** ⚠️ **CRITICAL**
**Why Missing**: Referenced in Employee but not implemented  
**Impact**: Cannot manage job roles, salary ranges, career progression

```java
@Entity
@Table(name = "positions",
       uniqueConstraints = @UniqueConstraint(columnNames = {"client_id", "code"}))
public class Position extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;
    
    @Column(nullable = false, length = 100)
    private String title;
    
    @Column(nullable = false, length = 20)
    private String code; // e.g., "SWE1", "MGR2"
    
    @Column(length = 1000)
    private String description;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "minSalary", column = @Column(name = "min_salary")),
        @AttributeOverride(name = "maxSalary", column = @Column(name = "max_salary"))
    })
    private SalaryRange salaryRange;
    
    @Column(name = "job_level")
    private Integer jobLevel; // 1, 2, 3, etc.
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PositionStatusEnum status = PositionStatusEnum.ACTIVE;
    
    // Soft delete
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
```

**Required Embeddable**:
```java
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SalaryRange implements Serializable {
    @Column(name = "min_salary", precision = 15, scale = 2)
    private BigDecimal minSalary;
    
    @Column(name = "max_salary", precision = 15, scale = 2)
    private BigDecimal maxSalary;
    
    @Column(name = "currency", length = 3)
    private String currency = "USD";
    
    public boolean isWithinRange(BigDecimal salary) {
        return salary.compareTo(minSalary) >= 0 && 
               salary.compareTo(maxSalary) <= 0;
    }
}
```

---

#### **3. PayPeriod Entity** ⚠️ **HIGH PRIORITY**
**Why Missing**: Needed for payroll scheduling and timesheet organization  
**Impact**: Hard to track pay periods, timesheet boundaries unclear

```java
@Entity
@Table(name = "pay_periods", indexes = {
    @Index(name = "idx_pay_period_client", columnList = "client_id"),
    @Index(name = "idx_pay_period_dates", columnList = "start_date, end_date")
})
public class PayPeriod extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "startDate", column = @Column(name = "start_date")),
        @AttributeOverride(name = "endDate", column = @Column(name = "end_date"))
    })
    private DatePeriod period;
    
    @Column(name = "pay_date", nullable = false)
    private LocalDate payDate;
    
    @Column(name = "period_number")
    private Integer periodNumber; // 1-52 for weekly, 1-26 for bi-weekly
    
    @Column(name = "fiscal_year")
    private Integer fiscalYear;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PayPeriodStatusEnum status = PayPeriodStatusEnum.OPEN;
    
    @Column(name = "is_locked")
    private Boolean isLocked = false;
    
    @Column(name = "locked_at")
    private LocalDateTime lockedAt;
    
    @Column(name = "locked_by")
    private String lockedBy;
    
    public boolean isActive() {
        LocalDate today = LocalDate.now();
        return !today.isBefore(period.getStartDate()) && 
               !today.isAfter(period.getEndDate());
    }
}
```

**Required Enum**:
```java
public enum PayPeriodStatusEnum {
    OPEN,        // Timesheet entry allowed
    PROCESSING,  // Payroll being calculated
    CLOSED       // Locked, no changes
}
```

---

#### **4. Timesheet Entity** ⚠️ **HIGH PRIORITY**
**Why Missing**: Time entries need to be grouped into weekly/bi-weekly timesheets  
**Impact**: Hard to submit/approve time entries in bulk

```java
@Entity
@Table(name = "timesheets", indexes = {
    @Index(name = "idx_timesheet_employee", columnList = "employee_id"),
    @Index(name = "idx_timesheet_pay_period", columnList = "pay_period_id"),
    @Index(name = "idx_timesheet_status", columnList = "status")
})
public class Timesheet extends BaseEntity {
    
    @Column(name = "employee_id", nullable = false)
    private String employeeId; // Reference to Employee microservice
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pay_period_id")
    private PayPeriod payPeriod;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "startDate", column = @Column(name = "week_start_date")),
        @AttributeOverride(name = "endDate", column = @Column(name = "week_end_date"))
    })
    private DatePeriod weekPeriod;
    
    @OneToMany(mappedBy = "timesheet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TimeEntry> timeEntries = new ArrayList<>();
    
    @Column(name = "total_regular_hours", precision = 10, scale = 2)
    private BigDecimal totalRegularHours = BigDecimal.ZERO;
    
    @Column(name = "total_overtime_hours", precision = 10, scale = 2)
    private BigDecimal totalOvertimeHours = BigDecimal.ZERO;
    
    @Column(name = "total_double_time_hours", precision = 10, scale = 2)
    private BigDecimal totalDoubleTimeHours = BigDecimal.ZERO;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TimesheetStatusEnum status = TimesheetStatusEnum.DRAFT;
    
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;
    
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
    
    @Column(length = 500)
    private String rejectionReason;
    
    public void calculateTotals() {
        this.totalRegularHours = timeEntries.stream()
            .map(TimeEntry::getRegularHours)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
            
        this.totalOvertimeHours = timeEntries.stream()
            .map(TimeEntry::getOvertimeHours)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
            
        this.totalDoubleTimeHours = timeEntries.stream()
            .map(TimeEntry::getDoubleTimeHours)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    public void submit() {
        if (this.status != TimesheetStatusEnum.DRAFT) {
            throw new IllegalStateException("Only DRAFT timesheets can be submitted");
        }
        calculateTotals();
        this.status = TimesheetStatusEnum.SUBMITTED;
        this.submittedAt = LocalDateTime.now();
    }
}
```

**Required Enum**:
```java
public enum TimesheetStatusEnum {
    DRAFT,
    SUBMITTED,
    APPROVED,
    REJECTED
}
```

---

#### **5. EmployeeBankAccount Entity** ⚠️ **CRITICAL**
**Why Missing**: Needed for direct deposit  
**Impact**: Cannot pay employees via direct deposit

```java
@Entity
@Table(name = "employee_bank_accounts", indexes = {
    @Index(name = "idx_bank_account_employee", columnList = "employee_id")
})
public class EmployeeBankAccount extends BaseEntity {
    
    @Column(name = "employee_id", nullable = false)
    private String employeeId;
    
    @Embedded
    private BankingInfo bankingInfo;
    
    @Column(name = "is_primary")
    private Boolean isPrimary = false;
    
    @Column(name = "allocation_percentage", precision = 5, scale = 2)
    private BigDecimal allocationPercentage = BigDecimal.valueOf(100);
    
    @Column(name = "allocation_fixed_amount", precision = 12, scale = 2)
    private BigDecimal allocationFixedAmount;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private BankAccountStatusEnum status = BankAccountStatusEnum.PENDING_VERIFICATION;
    
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
    
    @Column(name = "verification_method", length = 50)
    private String verificationMethod; // MICRO_DEPOSIT, PLAID, MANUAL
}
```

**Required Enum**:
```java
public enum BankAccountStatusEnum {
    PENDING_VERIFICATION,
    ACTIVE,
    INACTIVE,
    FAILED_VERIFICATION
}
```

---

#### **6. TaxWithholding Entity** ⚠️ **CRITICAL**
**Why Missing**: W-4 form data storage  
**Impact**: Cannot calculate accurate tax withholdings

```java
@Entity
@Table(name = "tax_withholdings", indexes = {
    @Index(name = "idx_tax_withholding_employee", columnList = "employee_id")
})
public class TaxWithholding extends BaseEntity {
    
    @Column(name = "employee_id", nullable = false)
    private String employeeId;
    
    @Embedded
    private TaxInfo taxInfo;
    
    @Column(name = "state_filing_status", length = 30)
    private String stateFilingStatus;
    
    @Column(name = "state_exemptions")
    private Integer stateExemptions = 0;
    
    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;
    
    @Column(name = "end_date")
    private LocalDate endDate;
    
    @Column(name = "w4_form_year")
    private Integer w4FormYear; // 2020, 2021, etc.
    
    @Column(name = "w4_document_url")
    private String w4DocumentUrl;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
}
```

---

#### **7. PayrollTaxDeposit Entity** ⚠️ **HIGH PRIORITY**
**Why Missing**: Track employer tax liabilities  
**Impact**: Cannot track tax deposits, compliance risk

```java
@Entity
@Table(name = "payroll_tax_deposits", indexes = {
    @Index(name = "idx_tax_deposit_client", columnList = "client_id"),
    @Index(name = "idx_tax_deposit_due_date", columnList = "due_date"),
    @Index(name = "idx_tax_deposit_status", columnList = "status")
})
public class PayrollTaxDeposit extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payroll_run_id")
    private PayrollRun payrollRun;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "tax_type", nullable = false, length = 50)
    private TaxTypeEnum taxType;
    
    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;
    
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;
    
    @Column(name = "paid_date")
    private LocalDate paidDate;
    
    @Column(name = "payment_reference", length = 100)
    private String paymentReference;
    
    @Column(name = "payment_method", length = 50)
    private String paymentMethod; // ACH, CHECK, WIRE
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TaxDepositStatusEnum status = TaxDepositStatusEnum.PENDING;
    
    @Column(name = "quarter")
    private Integer quarter; // 1, 2, 3, 4
    
    @Column(name = "tax_year")
    private Integer taxYear;
}
```

**Required Enum**:
```java
public enum TaxDepositStatusEnum {
    PENDING,
    SCHEDULED,
    PAID,
    OVERDUE,
    FAILED
}
```

---

#### **8. EmployeeCompensation Entity** ⚠️ **HIGH PRIORITY**
**Why Missing**: Track compensation history and changes  
**Impact**: No audit trail of salary changes

```java
@Entity
@Table(name = "employee_compensations", indexes = {
    @Index(name = "idx_compensation_employee", columnList = "employee_id"),
    @Index(name = "idx_compensation_effective_date", columnList = "effective_date")
})
public class EmployeeCompensation extends BaseEntity {
    
    @Column(name = "employee_id", nullable = false)
    private String employeeId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "compensation_type", nullable = false, length = 30)
    private CompensationTypeEnum compensationType;
    
    @Column(name = "amount", precision = 15, scale = 2, nullable = false)
    private BigDecimal amount;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "pay_frequency", length = 20)
    private PayFrequencyEnum payFrequency;
    
    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;
    
    @Column(name = "end_date")
    private LocalDate endDate;
    
    @Column(name = "is_active")
    private Boolean isActive = true;
    
    @Column(name = "change_reason", length = 200)
    private String changeReason; // "Annual increase", "Promotion", etc.
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;
    
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
}
```

---

#### **9. EmployeeDocument Entity** ⚠️ **MEDIUM PRIORITY**
**Why Missing**: Store employee documents (W-4, I-9, etc.)  
**Impact**: No document management

```java
@Entity
@Table(name = "employee_documents", indexes = {
    @Index(name = "idx_employee_doc_employee", columnList = "employee_id"),
    @Index(name = "idx_employee_doc_type", columnList = "document_type")
})
public class EmployeeDocument extends BaseEntity {
    
    @Column(name = "employee_id", nullable = false)
    private String employeeId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 50)
    private DocumentTypeEnum documentType;
    
    @Column(name = "file_name", nullable = false)
    private String fileName;
    
    @Column(name = "file_url", nullable = false)
    private String fileUrl;
    
    @Column(name = "file_size")
    private Long fileSize;
    
    @Column(name = "content_type", length = 100)
    private String contentType;
    
    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;
    
    @Column(name = "expiry_date")
    private LocalDate expiryDate;
    
    @Column(name = "is_verified")
    private Boolean isVerified = false;
}
```

**Required Enum**:
```java
public enum DocumentTypeEnum {
    W4,
    W2,
    I9,
    OFFER_LETTER,
    EMPLOYMENT_CONTRACT,
    NDA,
    ID_PROOF,
    PASSPORT,
    VISA,
    WORK_PERMIT,
    BACKGROUND_CHECK,
    DRUG_TEST,
    DIRECT_DEPOSIT_FORM,
    BENEFITS_ENROLLMENT,
    PERFORMANCE_REVIEW,
    TERMINATION_LETTER,
    OTHER
}
```

---

#### **10. PayrollHistory Entity** ⚠️ **HIGH PRIORITY**
**Why Missing**: Audit trail for payroll actions  
**Impact**: No audit trail, compliance risk

```java
@Entity
@Table(name = "payroll_history", indexes = {
    @Index(name = "idx_payroll_history_run", columnList = "payroll_run_id"),
    @Index(name = "idx_payroll_history_date", columnList = "performed_at")
})
public class PayrollHistory extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payroll_run_id", nullable = false)
    private PayrollRun payrollRun;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 30)
    private PayrollActionEnum action;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by")
    private User performedBy;
    
    @Column(name = "performed_at", nullable = false)
    private LocalDateTime performedAt;
    
    @Column(length = 1000)
    private String notes;
    
    @Column(name = "previous_status", length = 20)
    private String previousStatus;
    
    @Column(name = "new_status", length = 20)
    private String newStatus;
    
    @Column(name = "change_data", columnDefinition = "TEXT")
    private String changeData; // JSON snapshot
    
    @Column(name = "ip_address", length = 50)
    private String ipAddress;
}
```

**Required Enum**:
```java
public enum PayrollActionEnum {
    CREATED,
    PROCESSED,
    APPROVED,
    VOIDED,
    RECALCULATED,
    EXPORTED,
    PAID,
    REVERSED
}
```

---

#### **11. LeaveBalance Entity** ⚠️ **MEDIUM PRIORITY**
**Why Missing**: Track available leave hours/days  
**Impact**: Cannot enforce leave limits

```java
@Entity
@Table(name = "leave_balances", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "leave_type", "year"}))
public class LeaveBalance extends BaseEntity {
    
    @Column(name = "employee_id", nullable = false)
    private String employeeId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "leave_type", nullable = false, length = 30)
    private LeaveTypeEnum leaveType;
    
    @Column(nullable = false)
    private Integer year;
    
    @Column(name = "accrued_hours", precision = 8, scale = 2)
    private BigDecimal accruedHours = BigDecimal.ZERO;
    
    @Column(name = "used_hours", precision = 8, scale = 2)
    private BigDecimal usedHours = BigDecimal.ZERO;
    
    @Column(name = "available_hours", precision = 8, scale = 2)
    private BigDecimal availableHours = BigDecimal.ZERO;
    
    @Column(name = "carryover_hours", precision = 8, scale = 2)
    private BigDecimal carryoverHours = BigDecimal.ZERO;
    
    @Column(name = "max_carryover_hours", precision = 8, scale = 2)
    private BigDecimal maxCarryoverHours;
    
    @Column(name = "last_accrual_date")
    private LocalDate lastAccrualDate;
    
    public void recalculateAvailable() {
        this.availableHours = accruedHours
            .add(carryoverHours)
            .subtract(usedHours);
    }
}
```

---

#### **12. PayrollExport Entity** ⚠️ **MEDIUM PRIORITY**
**Why Missing**: Track payroll exports to accounting systems  
**Impact**: Manual reconciliation required

```java
@Entity
@Table(name = "payroll_exports")
public class PayrollExport extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payroll_run_id")
    private PayrollRun payrollRun;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "export_type", length = 50)
    private ExportTypeEnum exportType; // QUICKBOOKS, CSV, ACH_FILE
    
    @Column(name = "file_name")
    private String fileName;
    
    @Column(name = "file_url")
    private String fileUrl;
    
    @Column(name = "exported_at")
    private LocalDateTime exportedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exported_by")
    private User exportedBy;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ExportStatusEnum status;
}
```

**Required Enums**:
```java
public enum ExportTypeEnum {
    QUICKBOOKS_IIF,
    QUICKBOOKS_QBO,
    CSV,
    EXCEL,
    ACH_NACHA,
    JSON,
    XML
}

public enum ExportStatusEnum {
    PENDING,
    COMPLETED,
    FAILED
}
```

---

#### **13. OvertimeRule Entity** (mentioned in model but may not be implemented)
**Why Needed**: Configure overtime calculation rules per client  
**Impact**: Overtime may be calculated incorrectly

```java
@Entity
@Table(name = "overtime_rules")
public class OvertimeRule extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;
    
    @Column(nullable = false, length = 100)
    private String ruleName;
    
    @Column(name = "daily_overtime_threshold", precision = 5, scale = 2)
    private BigDecimal dailyOvertimeThreshold; // 8.0 hours
    
    @Column(name = "weekly_overtime_threshold", precision = 5, scale = 2)
    private BigDecimal weeklyOvertimeThreshold; // 40.0 hours
    
    @Column(name = "overtime_multiplier", precision = 5, scale = 2)
    private BigDecimal overtimeMultiplier = BigDecimal.valueOf(1.5);
    
    @Column(name = "double_time_threshold", precision = 5, scale = 2)
    private BigDecimal doubleTimeThreshold; // 12.0 hours/day or 60/week
    
    @Column(name = "double_time_multiplier", precision = 5, scale = 2)
    private BigDecimal doubleTimeMultiplier = BigDecimal.valueOf(2.0);
    
    @Column(name = "effective_date")
    private LocalDate effectiveDate;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RuleStatusEnum status = RuleStatusEnum.ACTIVE;
}
```

---

#### **14. Commission Entity** (mentioned in model)
**Why Needed**: Sales compensation tracking  
**Impact**: Cannot pay commission-based employees

```java
@Entity
@Table(name = "commissions", indexes = {
    @Index(name = "idx_commission_employee", columnList = "employee_id"),
    @Index(name = "idx_commission_pay_period", columnList = "pay_period_id")
})
public class Commission extends BaseEntity {
    
    @Column(name = "employee_id", nullable = false)
    private String employeeId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pay_period_id")
    private PayPeriod payPeriod;
    
    @Column(name = "sales_amount", precision = 15, scale = 2)
    private BigDecimal salesAmount;
    
    @Column(name = "commission_rate", precision = 5, scale = 2)
    private BigDecimal commissionRate; // Percentage
    
    @Column(name = "commission_amount", precision = 15, scale = 2)
    private BigDecimal commissionAmount;
    
    @Column(name = "sale_date")
    private LocalDate saleDate;
    
    @Column(name = "sale_reference", length = 100)
    private String saleReference;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CommissionStatusEnum status = CommissionStatusEnum.PENDING;
    
    @Column(length = 500)
    private String notes;
}
```

---

#### **15. PerformanceReview Entity** (mentioned in model)
**Why Needed**: Track employee performance (affects compensation)  
**Impact**: Cannot track performance history

```java
@Entity
@Table(name = "performance_reviews", indexes = {
    @Index(name = "idx_review_employee", columnList = "employee_id"),
    @Index(name = "idx_review_date", columnList = "review_date")
})
public class PerformanceReview extends BaseEntity {
    
    @Column(name = "employee_id", nullable = false)
    private String employeeId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "review_type", length = 30)
    private ReviewTypeEnum reviewType; // ANNUAL, PROBATION, PROMOTION
    
    @Column(name = "review_date", nullable = false)
    private LocalDate reviewDate;
    
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "startDate", column = @Column(name = "review_period_start")),
        @AttributeOverride(name = "endDate", column = @Column(name = "review_period_end"))
    })
    private DatePeriod reviewPeriod;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    private User reviewer;
    
    @Column(name = "overall_rating", precision = 3, scale = 1)
    private BigDecimal overallRating; // 1.0 to 5.0
    
    @Column(columnDefinition = "TEXT")
    private String strengths;
    
    @Column(columnDefinition = "TEXT")
    private String areasForImprovement;
    
    @Column(columnDefinition = "TEXT")
    private String goals;
    
    @Column(name = "recommended_increase", precision = 5, scale = 2)
    private BigDecimal recommendedIncreasePercentage;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ReviewStatusEnum status = ReviewStatusEnum.DRAFT;
    
    @Column(name = "employee_acknowledged_at")
    private LocalDateTime employeeAcknowledgedAt;
}
```

---

#### **16. EquityGrant Entity** (mentioned in model)
**Why Needed**: Track stock options, RSUs  
**Impact**: Cannot manage equity compensation

```java
@Entity
@Table(name = "equity_grants", indexes = {
    @Index(name = "idx_equity_employee", columnList = "employee_id")
})
public class EquityGrant extends BaseEntity {
    
    @Column(name = "employee_id", nullable = false)
    private String employeeId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "equity_type", length = 30)
    private EquityTypeEnum equityType; // STOCK_OPTION, RSU, ISO, NSO
    
    @Column(name = "grant_date", nullable = false)
    private LocalDate grantDate;
    
    @Column(name = "shares_granted")
    private Integer sharesGranted;
    
    @Column(name = "strike_price", precision = 15, scale = 4)
    private BigDecimal strikePrice;
    
    @Column(name = "vesting_start_date")
    private LocalDate vestingStartDate;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "vesting_schedule", length = 30)
    private VestingScheduleEnum vestingSchedule;
    
    @Column(name = "vesting_cliff_months")
    private Integer vestingCliffMonths; // 12 months typical
    
    @Column(name = "vesting_period_months")
    private Integer vestingPeriodMonths; // 48 months typical
    
    @Column(name = "shares_vested")
    private Integer sharesVested = 0;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private GrantStatusEnum status = GrantStatusEnum.ACTIVE;
}
```

---

#### **17. ShiftDifferential Entity** (mentioned in model)
**Why Needed**: Pay premiums for night/weekend shifts  
**Impact**: Cannot pay shift differentials

```java
@Entity
@Table(name = "shift_differentials")
public class ShiftDifferential extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;
    
    @Column(nullable = false, length = 100)
    private String name; // "Night Shift", "Weekend Premium"
    
    @Enumerated(EnumType.STRING)
    @Column(name = "differential_type", length = 30)
    private DifferentialTypeEnum differentialType; // PERCENTAGE, FIXED_AMOUNT
    
    @Column(precision = 10, scale = 2)
    private BigDecimal amount;
    
    @Column(name = "start_time")
    private LocalTime startTime;
    
    @Column(name = "end_time")
    private LocalTime endTime;
    
    @Column(name = "days_of_week", length = 50)
    private String daysOfWeek; // "SAT,SUN" or "MON-FRI"
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private DifferentialStatusEnum status = DifferentialStatusEnum.ACTIVE;
}
```

---

## 📦 **Complete DTO Structure Required**

### **Employee Domain DTOs**

#### **Request DTOs**
```java
// dto/employee/request/EmployeeCreateRequest.java
@Data
@Builder
public class EmployeeCreateRequest {
    @NotBlank private String firstName;
    @NotBlank private String lastName;
    @Email private String email;
    @Pattern(regexp = "^\\d{9}$") private String ssn;
    @NotNull private LocalDate dateOfBirth;
    @NotNull private LocalDate hireDate;
    @NotNull private EmploymentTypeEnum employmentType;
    @NotNull private PayTypeEnum payType;
    @NotNull private BigDecimal payRate;
    private UUID departmentId;
    private UUID positionId;
    @Valid private AddressDTO address;
    @Valid private BankingInfoDTO bankingInfo;
}

// dto/employee/request/EmployeeUpdateRequest.java
@Data
@Builder
public class EmployeeUpdateRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private AddressDTO address;
    private UUID departmentId;
    private UUID positionId;
    private EmploymentTypeEnum employmentType;
    private EmployeeStatusEnum status;
}

// dto/employee/request/EmployeeTerminateRequest.java
@Data
@Builder
public class EmployeeTerminateRequest {
    @NotNull private LocalDate terminationDate;
    @NotBlank private String terminationReason;
    private String notes;
}
```

#### **Response DTOs**
```java
// dto/employee/response/EmployeeResponse.java
@Data
@Builder
public class EmployeeResponse {
    private UUID id;
    private String employeeNumber;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private EmploymentTypeEnum employmentType;
    private EmployeeStatusEnum status;
    private LocalDate hireDate;
    private DepartmentSummary department;
    private PositionSummary position;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    @Data
    @Builder
    public static class DepartmentSummary {
        private UUID id;
        private String name;
        private String code;
    }
    
    @Data
    @Builder
    public static class PositionSummary {
        private UUID id;
        private String title;
        private String code;
    }
}

// dto/employee/response/EmployeeDetailResponse.java
@Data
@Builder
public class EmployeeDetailResponse {
    private UUID id;
    private String employeeNumber;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private AddressDTO address;
    private LocalDate dateOfBirth;
    private LocalDate hireDate;
    private LocalDate terminationDate;
    private EmploymentTypeEnum employmentType;
    private PayTypeEnum payType;
    private BigDecimal payRate;
    private EmployeeStatusEnum status;
    private DepartmentDetail department;
    private PositionDetail position;
    private CompensationSummary currentCompensation;
    private List<BankAccountSummary> bankAccounts;
    private TaxWithholdingSummary taxWithholding;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

---

### **Payroll Domain DTOs**

#### **Request DTOs**
```java
// dto/payroll/request/PayrollRunCreateRequest.java
@Data
@Builder
public class PayrollRunCreateRequest {
    @NotNull private UUID clientId;
    @NotNull private UUID payPeriodId;
    @NotNull private LocalDate payDate;
    @NotNull private PayrollTypeEnum payrollType;
    private String description;
    private List<UUID> includedEmployeeIds; // Optional: specific employees
}

// dto/payroll/request/PayrollAdjustmentRequest.java
@Data
@Builder
public class PayrollAdjustmentRequest {
    @NotBlank private String employeeId;
    @NotNull private AdjustmentTypeEnum adjustmentType;
    @NotNull private BigDecimal amount;
    @NotBlank private String description;
    private String reason;
}
```

#### **Response DTOs**
```java
// dto/payroll/response/PayrollRunResponse.java
@Data
@Builder
public class PayrollRunResponse {
    private UUID id;
    private ClientSummary client;
    private PayPeriodSummary payPeriod;
    private LocalDate payDate;
    private PayrollTypeEnum payrollType;
    private PayrollStatusEnum status;
    private PayrollTotalsDTO totals;
    private Integer employeeCount;
    private LocalDateTime processedAt;
    private String processedBy;
    private LocalDateTime approvedAt;
    private String approvedBy;
    private LocalDateTime createdAt;
}

// dto/payroll/response/PaystubResponse.java
@Data
@Builder
public class PaystubResponse {
    private UUID id;
    private String employeeNumber;
    private String employeeName;
    private PayPeriodSummary payPeriod;
    private LocalDate payDate;
    private String checkNumber;
    
    // Earnings
    private List<EarningDetail> earnings;
    private BigDecimal totalEarnings;
    
    // Deductions
    private List<DeductionDetail> deductions;
    private BigDecimal totalDeductions;
    
    // Taxes
    private List<TaxDetail> taxes;
    private BigDecimal totalTaxes;
    
    // Net pay
    private BigDecimal netPay;
    
    // YTD totals
    private YtdTotalsDTO ytdTotals;
    
    private String pdfUrl;
}
```

---

### **Timesheet Domain DTOs**

```java
// dto/timesheet/request/TimeEntryRequest.java
@Data
@Builder
public class TimeEntryRequest {
    @NotNull private LocalDate workDate;
    @NotNull private LocalTime clockIn;
    private LocalTime clockOut;
    private Integer breakMinutes;
    private String projectCode;
    private String notes;
}

// dto/timesheet/response/TimesheetResponse.java
@Data
@Builder
public class TimesheetResponse {
    private UUID id;
    private String employeeId;
    private String employeeName;
    private PayPeriodSummary payPeriod;
    private DatePeriodDTO weekPeriod;
    private List<TimeEntryResponse> timeEntries;
    private BigDecimal totalRegularHours;
    private BigDecimal totalOvertimeHours;
    private TimesheetStatusEnum status;
    private LocalDateTime submittedAt;
    private String approvedBy;
    private LocalDateTime approvedAt;
}
```

---

### **Leave Domain DTOs**

```java
// dto/leave/request/LeaveRequestCreate.java
@Data
@Builder
public class LeaveRequestCreate {
    @NotNull private LeaveTypeEnum leaveType;
    @NotNull private LocalDate startDate;
    @NotNull private LocalDate endDate;
    private String reason;
    private Boolean isPaid;
}

// dto/leave/response/LeaveBalanceResponse.java
@Data
@Builder
public class LeaveBalanceResponse {
    private String employeeId;
    private Integer year;
    private Map<LeaveTypeEnum, LeaveBalance> balances;
    
    @Data
    @Builder
    public static class LeaveBalance {
        private LeaveTypeEnum leaveType;
        private BigDecimal accrued;
        private BigDecimal used;
        private BigDecimal available;
        private BigDecimal carryover;
    }
}
```

---

## 🔧 **Missing Services**

### **1. TimeEntryService**
```java
@Service
public class TimeEntryService {
    public TimeEntryResponse createTimeEntry(String employeeId, TimeEntryRequest request);
    public TimeEntryResponse updateTimeEntry(UUID id, TimeEntryRequest request);
    public void deleteTimeEntry(UUID id);
    public TimeEntryResponse approveTimeEntry(UUID id, UUID approverId);
    public TimeEntryResponse rejectTimeEntry(UUID id, String reason);
    public Page<TimeEntryResponse> getEmployeeTimeEntries(String employeeId, Pageable pageable);
    public TimeEntryResponse clockIn(String employeeId, ClockInRequest request);
    public TimeEntryResponse clockOut(String employeeId);
    public TimeEntryResponse getCurrentTimeEntry(String employeeId);
}
```

### **2. TimesheetService**
```java
@Service
public class TimesheetService {
    public TimesheetResponse createTimesheet(String employeeId, UUID payPeriodId);
    public TimesheetResponse getTimesheet(UUID id);
    public TimesheetResponse submitTimesheet(UUID id);
    public TimesheetResponse approveTimesheet(UUID id, UUID approverId);
    public TimesheetResponse rejectTimesheet(UUID id, String reason);
    public Page<TimesheetResponse> getEmployeeTimesheets(String employeeId, Pageable pageable);
    public List<TimesheetResponse> getPendingApprovals(UUID clientId);
}
```

### **3. LeaveRequestService**
```java
@Service
public class LeaveRequestService {
    public LeaveRequestResponse createLeaveRequest(String employeeId, LeaveRequestCreate request);
    public LeaveRequestResponse getLeaveRequest(UUID id);
    public LeaveRequestResponse updateLeaveRequest(UUID id, LeaveRequestCreate request);
    public void cancelLeaveRequest(UUID id);
    public LeaveRequestResponse approveLeaveRequest(UUID id, UUID approverId);
    public LeaveRequestResponse rejectLeaveRequest(UUID id, String reason);
    public Page<LeaveRequestResponse> getEmployeeLeaveRequests(String employeeId, Pageable pageable);
    public LeaveBalanceResponse getLeaveBalance(String employeeId, Integer year);
}
```

### **4. PaystubService**
```java
@Service
public class PaystubService {
    public List<PaystubResponse> generatePaystubs(UUID payrollRunId);
    public PaystubResponse getPaystub(UUID id);
    public byte[] generatePaystubPdf(UUID id);
    public Page<PaystubResponse> getEmployeePaystubs(String employeeId, Pageable pageable);
    public YtdSummaryResponse getYtdSummary(String employeeId, Integer year);
}
```

### **5. BenefitService**
```java
@Service
public class BenefitService {
    public BenefitPlanResponse createBenefitPlan(BenefitPlanRequest request);
    public BenefitPlanResponse updateBenefitPlan(UUID id, BenefitPlanRequest request);
    public void deleteBenefitPlan(UUID id);
    public Page<BenefitPlanResponse> getAllBenefitPlans(UUID clientId, Pageable pageable);
    
    public BenefitEnrollmentResponse enrollEmployee(String employeeId, BenefitEnrollmentRequest request);
    public void unenrollEmployee(UUID enrollmentId);
    public List<BenefitEnrollmentResponse> getEmployeeBenefits(String employeeId);
    public BigDecimal calculateTotalDeductions(String employeeId);
}
```

### **6. TaxService**
```java
@Service
public class TaxService {
    public TaxWithholdingResponse configureTaxWithholding(String employeeId, TaxWithholdingRequest request);
    public TaxWithholdingResponse getTaxWithholding(String employeeId);
    public TaxCalculationResponse calculateTaxes(PayrollCalculationContext context);
    public List<TaxDepositResponse> getUpcomingTaxDeposits(UUID clientId);
    public TaxDepositResponse recordTaxDeposit(UUID depositId, PaymentInfo payment);
}
```

### **7. DepartmentService**
```java
@Service
public class DepartmentService {
    public DepartmentResponse createDepartment(DepartmentRequest request);
    public DepartmentResponse updateDepartment(UUID id, DepartmentRequest request);
    public void deleteDepartment(UUID id);
    public Page<DepartmentResponse> getAllDepartments(UUID clientId, Pageable pageable);
    public List<EmployeeResponse> getDepartmentEmployees(UUID departmentId);
}
```

### **8. PositionService**
```java
@Service
public class PositionService {
    public PositionResponse createPosition(PositionRequest request);
    public PositionResponse updatePosition(UUID id, PositionRequest request);
    public void deletePosition(UUID id);
    public Page<PositionResponse> getAllPositions(UUID clientId, Pageable pageable);
}
```

### **9. ReportService**
```java
@Service
public class ReportService {
    public PayrollSummaryReport generatePayrollSummary(UUID payrollRunId);
    public TaxLiabilityReport generateTaxLiabilityReport(UUID clientId, Integer quarter, Integer year);
    public EmployeeEarningsReport generateEmployeeEarnings(String employeeId, Integer year);
    public byte[] exportReport(UUID reportId, ExportTypeEnum exportType);
    public QuarterEndReport generate941Report(UUID clientId, Integer quarter, Integer year);
    public List<W2Response> generateW2Forms(UUID clientId, Integer year);
}
```

---

## 🗄️ **Missing Repositories**

```java
public interface DepartmentRepository extends JpaRepository<Department, UUID> {
    Page<Department> findByClientId(UUID clientId, Pageable pageable);
    Optional<Department> findByClientIdAndCode(UUID clientId, String code);
    List<Department> findByStatus(DepartmentStatusEnum status);
}

public interface PositionRepository extends JpaRepository<Position, UUID> {
    Page<Position> findByClientId(UUID clientId, Pageable pageable);
    Optional<Position> findByClientIdAndCode(UUID clientId, String code);
    List<Position> findByDepartmentId(UUID departmentId);
}

public interface PayPeriodRepository extends JpaRepository<PayPeriod, UUID> {
    Page<PayPeriod> findByClientId(UUID clientId, Pageable pageable);
    Optional<PayPeriod> findByClientIdAndPeriodStartDateAndPeriodEndDate(
        UUID clientId, LocalDate start, LocalDate end);
    List<PayPeriod> findByStatus(PayPeriodStatusEnum status);
}

public interface TimesheetRepository extends JpaRepository<Timesheet, UUID> {
    Page<Timesheet> findByEmployeeId(String employeeId, Pageable pageable);
    Optional<Timesheet> findByEmployeeIdAndPayPeriodId(String employeeId, UUID payPeriodId);
    List<Timesheet> findByStatus(TimesheetStatusEnum status);
}

public interface TimeEntryRepository extends JpaRepository<TimeEntry, UUID> {
    Page<TimeEntry> findByEmployeeId(String employeeId, Pageable pageable);
    List<TimeEntry> findByTimesheetId(UUID timesheetId);
    Optional<TimeEntry> findByEmployeeIdAndWorkDate(String employeeId, LocalDate date);
}

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, UUID> {
    Page<LeaveRequest> findByEmployeeId(String employeeId, Pageable pageable);
    List<LeaveRequest> findByStatus(LeaveStatusEnum status);
    List<LeaveRequest> findByEmployeeIdAndStatusAndStartDateBetween(
        String employeeId, LeaveStatusEnum status, LocalDate start, LocalDate end);
}

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, UUID> {
    Optional<LeaveBalance> findByEmployeeIdAndLeaveTypeAndYear(
        String employeeId, LeaveTypeEnum leaveType, Integer year);
    List<LeaveBalance> findByEmployeeIdAndYear(String employeeId, Integer year);
}

public interface PaystubRepository extends JpaRepository<Paystub, UUID> {
    Page<Paystub> findByEmployeeId(String employeeId, Pageable pageable);
    List<Paystub> findByPayrollRunId(UUID payrollRunId);
    Optional<Paystub> findByEmployeeIdAndPayrollRunId(String employeeId, UUID payrollRunId);
}

public interface BenefitPlanRepository extends JpaRepository<BenefitPlan, UUID> {
    Page<BenefitPlan> findByClientId(UUID clientId, Pageable pageable);
    List<BenefitPlan> findByStatus(PlanStatusEnum status);
}

public interface BenefitEnrollmentRepository extends JpaRepository<BenefitEnrollment, UUID> {
    List<BenefitEnrollment> findByEmployeeId(String employeeId);
    List<BenefitEnrollment> findByBenefitPlanId(UUID planId);
}

public interface TaxConfigurationRepository extends JpaRepository<TaxConfiguration, UUID> {
    Optional<TaxConfiguration> findByEmployeeId(String employeeId);
    List<TaxConfiguration> findByClientId(UUID clientId);
}

public interface TaxWithholdingRepository extends JpaRepository<TaxWithholding, UUID> {
    Optional<TaxWithholding> findByEmployeeIdAndIsActiveTrue(String employeeId);
    List<TaxWithholding> findByEmployeeId(String employeeId);
}

public interface DeductionRepository extends JpaRepository<Deduction, UUID> {
    List<Deduction> findByEmployeeId(String employeeId);
    List<Deduction> findByStatus(DeductionStatusEnum status);
}

public interface PayrollAdjustmentRepository extends JpaRepository<PayrollAdjustment, UUID> {
    List<PayrollAdjustment> findByEmployeeId(String employeeId);
    List<PayrollAdjustment> findByPayrollRunId(UUID payrollRunId);
}

public interface PayrollHistoryRepository extends JpaRepository<PayrollHistory, UUID> {
    List<PayrollHistory> findByPayrollRunIdOrderByPerformedAtDesc(UUID payrollRunId);
}

public interface EmployeeBankAccountRepository extends JpaRepository<EmployeeBankAccount, UUID> {
    List<EmployeeBankAccount> findByEmployeeId(String employeeId);
    Optional<EmployeeBankAccount> findByEmployeeIdAndIsPrimaryTrue(String employeeId);
}

public interface EmployeeDocumentRepository extends JpaRepository<EmployeeDocument, UUID> {
    List<EmployeeDocument> findByEmployeeId(String employeeId);
    List<EmployeeDocument> findByEmployeeIdAndDocumentType(String employeeId, DocumentTypeEnum type);
}

public interface PayrollTaxDepositRepository extends JpaRepository<PayrollTaxDeposit, UUID> {
    List<PayrollTaxDeposit> findByClientId(UUID clientId);
    List<PayrollTaxDeposit> findByStatus(TaxDepositStatusEnum status);
    List<PayrollTaxDeposit> findByDueDateBetween(LocalDate start, LocalDate end);
}

public interface EmployeeCompensationRepository extends JpaRepository<EmployeeCompensation, UUID> {
    List<EmployeeCompensation> findByEmployeeIdOrderByEffectiveDateDesc(String employeeId);
    Optional<EmployeeCompensation> findByEmployeeIdAndIsActiveTrue(String employeeId);
}

public interface CommissionRepository extends JpaRepository<Commission, UUID> {
    List<Commission> findByEmployeeId(String employeeId);
    List<Commission> findByPayPeriodId(UUID payPeriodId);
}

public interface PerformanceReviewRepository extends JpaRepository<PerformanceReview, UUID> {
    List<PerformanceReview> findByEmployeeIdOrderByReviewDateDesc(String employeeId);
}

public interface EquityGrantRepository extends JpaRepository<EquityGrant, UUID> {
    List<EquityGrant> findByEmployeeId(String employeeId);
    List<EquityGrant> findByStatus(GrantStatusEnum status);
}

public interface ShiftDifferentialRepository extends JpaRepository<ShiftDifferential, UUID> {
    List<ShiftDifferential> findByClientIdAndStatus(UUID clientId, DifferentialStatusEnum status);
}

public interface PayrollExportRepository extends JpaRepository<PayrollExport, UUID> {
    List<PayrollExport> findByPayrollRunId(UUID payrollRunId);
    Page<PayrollExport> findByClientId(UUID clientId, Pageable pageable);
}
```

---

## 🎯 **Quick Wins (Implement First)**

### **Week 1 - Critical Path**
1. ✅ Fix entity consolidation (model → entity)
2. ✅ Create Department & Position entities
3. ✅ Create PayPeriod entity
4. ✅ Create all repositories
5. ✅ Update Employee entity to reference Department & Position

### **Week 2 - High Value**
6. ✅ Implement TimeEntry & Timesheet functionality
7. ✅ Implement LeaveRequest functionality
8. ✅ Create EmployeeBankAccount entity
9. ✅ Create TaxWithholding entity

### **Week 3 - Complete Payroll**
10. ✅ Enhance PaystubService
11. ✅ Implement TaxService
12. ✅ Create PayrollHistory tracking

---

## 🚨 **Critical Missing Business Logic**

### **1. Tax Calculation Engine**
**Current**: TaxConfiguration exists but no calculation logic  
**Needed**:
- Federal income tax calculation (2026 tax tables)
- State income tax calculation
- Social Security (6.2% up to wage base)
- Medicare (1.45% + 0.9% additional for high earners)
- State-specific taxes (unemployment, disability, etc.)

**Recommendation**: Use external library or tax API
- Gusto API
- ADP API
- Custom tax table implementation

### **2. Overtime Calculation**
**Current**: OvertimeRule may exist but not integrated  
**Needed**:
- Daily overtime (> 8 hours)
- Weekly overtime (> 40 hours)
- Double time (> 12 hours/day)
- State-specific rules (CA has unique rules)

### **3. Paystub PDF Generation**
**Current**: Paystub entity exists but no PDF generation  
**Needed**:
- PDF template
- YTD calculations
- Pay period details
- Earnings/deductions breakdown

**Recommendation**: Use iText, Apache PDFBox, or Jasper Reports

### **4. Direct Deposit / ACH File Generation**
**Current**: BankingInfo embedded but no ACH file generation  
**Needed**:
- NACHA file format generation
- Split deposit support
- Validation & verification

### **5. Leave Accrual Calculation**
**Current**: LeaveRequest exists but no balance tracking  
**Needed**:
- Automatic accrual (monthly, per pay period)
- Carryover logic
- Leave type policies (PTO, sick, vacation)

---

## 📐 **Entity Relationship Improvements**

### **Update TimeEntry to reference Timesheet**
```java
@Entity
public class TimeEntry extends BaseEntity {
    // Add this relationship
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "timesheet_id")
    private Timesheet timesheet;
    
    // ... existing fields
}
```

### **Update Employee to reference Department & Position**
```java
@Entity
public class Employee extends BaseEntity {
    // Add these relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "position_id")
    private Position position;
    
    // Remove these string fields
    // private String department;
    // private String position;
}
```

### **Update PayrollRun to reference PayPeriod**
```java
@Entity
public class PayrollRun extends BaseEntity {
    // Add this relationship
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pay_period_id")
    private PayPeriod payPeriod;
    
    // Can keep embedded DatePeriod for denormalized access
}
```

---

## 🧪 **Testing Strategy**

### **Unit Tests Needed**
- ✅ All service methods
- ✅ Entity business logic methods
- ✅ Mapper transformations
- ✅ Tax calculations
- ✅ Overtime calculations
- ✅ Leave balance calculations

### **Integration Tests Needed**
- ✅ Full payroll processing flow
- ✅ Time entry → timesheet → payroll
- ✅ Leave request approval flow
- ✅ Benefit enrollment flow
- ✅ Tax withholding updates

### **Performance Tests**
- ✅ Payroll processing for 1000+ employees
- ✅ Bulk time entry import
- ✅ Report generation

---

## 📋 **Complete Implementation Checklist**

### **Phase 1: Foundation (Days 1-7)**
- [ ] Move all `/model/` classes to `/entity/`
- [ ] Delete `/model/` folder
- [ ] Update all imports
- [ ] Create Department entity
- [ ] Create Position entity
- [ ] Create PayPeriod entity
- [ ] Create Timesheet entity
- [ ] Create all missing repositories
- [ ] Run application successfully
- [ ] All tests passing

### **Phase 2: Time & Attendance (Days 8-14)**
- [ ] Create TimeEntryService
- [ ] Create TimesheetService
- [ ] Create TimeEntryController
- [ ] Create TimesheetController
- [ ] Implement clock in/out
- [ ] Create time DTOs
- [ ] Write unit tests
- [ ] Integration tests

### **Phase 3: Leave Management (Days 15-21)**
- [ ] Create LeaveBalance entity
- [ ] Create LeaveRequestService
- [ ] Create LeaveRequestController
- [ ] Implement leave accrual
- [ ] Create leave DTOs
- [ ] Write unit tests
- [ ] Integration tests

### **Phase 4: Enhanced Payroll (Days 22-28)**
- [ ] Create PaystubService
- [ ] Create TaxService
- [ ] Create PayrollHistory entity
- [ ] Implement PDF generation
- [ ] Create payroll DTOs
- [ ] Write unit tests

### **Phase 5: Benefits (Days 29-35)**
- [ ] Create BenefitService
- [ ] Create BenefitController
- [ ] Implement enrollment flow
- [ ] Create benefit DTOs
- [ ] Write unit tests

### **Phase 6: Banking & Payments (Days 36-42)**
- [ ] Create EmployeeBankAccount entity
- [ ] Implement ACH file generation
- [ ] Create payment DTOs
- [ ] Write unit tests

### **Phase 7: Tax Management (Days 43-49)**
- [ ] Create TaxWithholding entity
- [ ] Create PayrollTaxDeposit entity
- [ ] Implement tax calculation engine
- [ ] Create tax DTOs
- [ ] Write unit tests

### **Phase 8: Reporting (Days 50-56)**
- [ ] Create ReportService
- [ ] Create ReportController
- [ ] Implement all reports
- [ ] Export functionality
- [ ] Write unit tests

---

## 🎓 **Best Practices Enforcement**

### **1. No Magic Strings**
❌ BAD: `status.equals("APPROVED")`  
✅ GOOD: `status == PayrollStatusEnum.APPROVED`

### **2. Use Embeddables**
❌ BAD: Separate fields for street, city, state, zip  
✅ GOOD: `@Embedded private Address address`

### **3. Use DTOs Everywhere**
❌ BAD: Return entity from controller  
✅ GOOD: Return Response DTO

### **4. Proper Relationships**
❌ BAD: `private String departmentId`  
✅ GOOD: `@ManyToOne private Department department`

### **5. Enum Naming**
❌ BAD: `public enum Status`  
✅ GOOD: `public enum PayrollStatusEnum`

---

**END OF ENTITY ANALYSIS**

This document provides the complete blueprint for implementing all missing components in WageCraft.
