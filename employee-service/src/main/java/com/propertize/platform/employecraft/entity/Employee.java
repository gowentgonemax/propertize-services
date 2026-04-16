package com.propertize.platform.employecraft.entity;

import com.propertize.platform.employecraft.entity.embedded.Address;
import com.propertize.platform.employecraft.entity.embedded.Compensation;
import com.propertize.platform.employecraft.entity.embedded.EmergencyContact;
import com.propertize.commons.enums.employee.EmployeeStatusEnum;
import com.propertize.commons.enums.employee.EmploymentTypeEnum;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Employee entity - Core HR entity
 * Links to Propertize via organizationId and optionally userId
 */
@Entity
@Table(name = "employees", indexes = {
        @Index(name = "idx_employee_org", columnList = "organization_id"),
        @Index(name = "idx_employee_number", columnList = "employee_number, organization_id", unique = true),
        @Index(name = "idx_employee_email", columnList = "email, organization_id", unique = true),
        @Index(name = "idx_employee_user", columnList = "user_id"),
        @Index(name = "idx_employee_status", columnList = "status"),
        @Index(name = "idx_employee_updated", columnList = "updated_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Employee extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // ===== PROPERTIZE INTEGRATION =====
    /**
     * Organization ID from Propertize - required for multi-tenancy
     */
    @Column(name = "organization_id", nullable = false, columnDefinition = "uuid")
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID organizationId;

    /**
     * User ID from Propertize - only if employee has system access
     * NULL means no login capability
     */
    @Column(name = "user_id")
    private Long userId;

    // ===== EMPLOYEE IDENTIFICATION =====
    @Column(name = "employee_number", nullable = false)
    private String employeeNumber;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "middle_name")
    private String middleName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(nullable = false)
    private String email;

    @Column(name = "work_email")
    private String workEmail;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "work_phone")
    private String workPhone;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    // SSN stored encrypted
    @Column(name = "ssn_encrypted")
    private String ssnEncrypted;

    // ===== EMPLOYMENT DETAILS =====
    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", nullable = false)
    private EmploymentTypeEnum employmentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EmployeeStatusEnum status = EmployeeStatusEnum.PENDING;

    @Column(name = "hire_date", nullable = false)
    private LocalDate hireDate;

    @Column(name = "termination_date")
    private LocalDate terminationDate;

    @Column(name = "termination_reason")
    private String terminationReason;

    @Column(name = "probation_end_date")
    private LocalDate probationEndDate;

    // ===== RELATIONSHIPS =====
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "position_id")
    private Position position;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reports_to")
    private Employee manager;

    // ===== EMBEDDED =====
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "streetAddress", column = @Column(name = "home_street")),
            @AttributeOverride(name = "city", column = @Column(name = "home_city")),
            @AttributeOverride(name = "state", column = @Column(name = "home_state")),
            @AttributeOverride(name = "zipCode", column = @Column(name = "home_zip")),
            @AttributeOverride(name = "country", column = @Column(name = "home_country"))
    })
    private Address homeAddress;

    @Embedded
    private Compensation compensation;

    @Embedded
    private EmergencyContact emergencyContact;

    // ===== METADATA =====
    @Column(name = "profile_photo_url")
    private String profilePhotoUrl;

    @Column(length = 2000)
    private String notes;

    @Version
    private Long version;

    // ===== HELPER METHODS =====
    public String getFullName() {
        StringBuilder sb = new StringBuilder();
        sb.append(firstName);
        if (middleName != null && !middleName.isBlank()) {
            sb.append(" ").append(middleName);
        }
        sb.append(" ").append(lastName);
        return sb.toString();
    }

    public boolean hasSystemAccess() {
        return userId != null;
    }

    public boolean isActive() {
        return status == EmployeeStatusEnum.ACTIVE;
    }

    public boolean isTerminated() {
        return status == EmployeeStatusEnum.TERMINATED;
    }
}
