package com.propertize.payroll.entity;

import com.propertize.payroll.entity.base.BaseEntity;
import com.propertize.payroll.entity.embedded.Address;
import com.propertize.payroll.entity.embedded.BankingInfo;
import com.propertize.payroll.entity.embedded.ContactInfo;
import com.propertize.payroll.enums.EmploymentTypeEnum;
import com.propertize.payroll.enums.EmployeeStatusEnum;
import com.propertize.payroll.enums.PayTypeEnum;
import com.propertize.payroll.enums.PayFrequencyEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing an employee within a client organization.
 * This entity stores employee data cached from the Employee Microservice
 * plus additional payroll-specific information.
 */
@Entity
@Table(name = "employees", indexes = {
        @Index(name = "idx_employee_client", columnList = "client_id"),
        @Index(name = "idx_employee_external_id", columnList = "externalEmployeeId"),
        @Index(name = "idx_employee_number", columnList = "employeeNumber"),
        @Index(name = "idx_employee_status", columnList = "status"),
        @Index(name = "idx_employee_ssn_last_four", columnList = "ssnLastFour"),
        @Index(name = "idx_employee_department", columnList = "departmentId")
})
@Getter
@Setter
@BatchSize(size = 25)
public class EmployeeEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    /**
     * External ID from Employee Microservice for cross-reference (UUID for
     * type-safe FK)
     */
    @Column(name = "external_employee_id")
    private UUID externalEmployeeId;

    @Column(nullable = false, unique = true, length = 50)
    private String employeeNumber;

    @Column(nullable = false)
    private String firstName;

    @Column
    private String middleName;

    @Column(nullable = false)
    private String lastName;

    @Column
    private String preferredName;

    /**
     * Last 4 digits of SSN for identification (full SSN stored encrypted)
     */
    @Column(length = 4)
    private String ssnLastFour;

    /**
     * Full SSN (encrypted) - required for tax reporting
     */
    @Column(length = 256)
    private String ssnEncrypted;

    @Column
    private LocalDate dateOfBirth;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "email", column = @Column(name = "employee_email")),
            @AttributeOverride(name = "phone", column = @Column(name = "employee_phone")),
            @AttributeOverride(name = "mobile", column = @Column(name = "employee_mobile")),
            @AttributeOverride(name = "fax", column = @Column(name = "employee_fax")),
            @AttributeOverride(name = "contactPerson", column = @Column(name = "emergency_contact"))
    })
    private ContactInfo contactInfo;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "street", column = @Column(name = "home_street")),
            @AttributeOverride(name = "city", column = @Column(name = "home_city")),
            @AttributeOverride(name = "state", column = @Column(name = "home_state")),
            @AttributeOverride(name = "zipCode", column = @Column(name = "home_zip_code")),
            @AttributeOverride(name = "country", column = @Column(name = "home_country")),
            @AttributeOverride(name = "addressType", column = @Column(name = "home_address_type"))
    })
    private Address homeAddress;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "bankName", column = @Column(name = "bank_name")),
            @AttributeOverride(name = "accountNumber", column = @Column(name = "bank_account_number")),
            @AttributeOverride(name = "routingNumber", column = @Column(name = "bank_routing_number")),
            @AttributeOverride(name = "accountType", column = @Column(name = "bank_account_type")),
            @AttributeOverride(name = "isPrimary", column = @Column(name = "is_primary_bank"))
    })
    private BankingInfo bankingInfo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmployeeStatusEnum status = EmployeeStatusEnum.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmploymentTypeEnum employmentType = EmploymentTypeEnum.FULL_TIME;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PayTypeEnum payType = PayTypeEnum.HOURLY;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PayFrequencyEnum payFrequency = PayFrequencyEnum.BI_WEEKLY;

    @Column(precision = 15, scale = 4)
    private BigDecimal hourlyRate;

    @Column(precision = 15, scale = 2)
    private BigDecimal annualSalary;

    @Column
    private LocalDate hireDate;

    @Column
    private LocalDate terminationDate;

    @Column
    private LocalDate lastRaiseDate;

    @Column(length = 100)
    private String jobTitle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "position_id")
    private PositionEntity position;

    @Column(length = 100)
    private String departmentId;

    @Column(length = 100)
    private String departmentName;

    @Column(length = 100)
    private String managerId;

    @Column(length = 100)
    private String managerName;

    @Column(length = 100)
    private String workLocation;

    @Column(length = 20)
    private String costCenter;

    /**
     * Standard work hours per week (default 40)
     */
    @Column(precision = 5, scale = 2)
    private BigDecimal standardHoursPerWeek = new BigDecimal("40.00");

    /**
     * Overtime multiplier (typically 1.5)
     */
    @Column(precision = 4, scale = 2)
    private BigDecimal overtimeMultiplier = new BigDecimal("1.50");

    /**
     * Whether employee is eligible for overtime
     */
    @Column(nullable = false)
    private Boolean overtimeEligible = true;

    // Relationships
    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Deduction> deductions = new ArrayList<>();

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BenefitEnrollment> benefitEnrollments = new ArrayList<>();

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TimeEntry> timeEntries = new ArrayList<>();

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LeaveRequest> leaveRequests = new ArrayList<>();

    /**
     * Returns the full name of the employee.
     */
    public String getFullName() {
        StringBuilder name = new StringBuilder(firstName);
        if (middleName != null && !middleName.isEmpty()) {
            name.append(" ").append(middleName);
        }
        name.append(" ").append(lastName);
        return name.toString();
    }

    /**
     * Returns the display name (preferred name or first name).
     */
    public String getDisplayName() {
        return preferredName != null && !preferredName.isEmpty() ? preferredName : firstName;
    }

    /**
     * Calculates the effective pay rate based on pay type.
     */
    public BigDecimal getEffectivePayRate() {
        if (payType == PayTypeEnum.SALARY && annualSalary != null) {
            // Calculate hourly rate from annual salary (assuming 2080 work hours per year)
            return annualSalary.divide(new BigDecimal("2080"), 4, java.math.RoundingMode.HALF_UP);
        }
        return hourlyRate != null ? hourlyRate : BigDecimal.ZERO;
    }

    /**
     * Checks if the employee is currently active.
     */
    public boolean isActive() {
        return status == EmployeeStatusEnum.ACTIVE;
    }

    /**
     * Checks if the employee is on leave.
     */
    public boolean isOnLeave() {
        return status == EmployeeStatusEnum.ON_LEAVE;
    }
}
