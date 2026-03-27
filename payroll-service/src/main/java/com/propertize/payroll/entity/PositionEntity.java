package com.propertize.payroll.entity;

import com.propertize.payroll.entity.base.BaseEntity;
import com.propertize.payroll.entity.embedded.SalaryRange;
import com.propertize.payroll.enums.PositionStatusEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entity representing a job position/role within a client organization.
 */
@Entity
@Table(name = "positions",
       indexes = {
           @Index(name = "idx_position_client", columnList = "client_id"),
           @Index(name = "idx_position_code", columnList = "position_code"),
           @Index(name = "idx_position_department", columnList = "department_id"),
           @Index(name = "idx_position_status", columnList = "status")
       },
       uniqueConstraints = @UniqueConstraint(columnNames = {"client_id", "position_code"}))
@Getter
@Setter
public class PositionEntity extends BaseEntity {

    /**
     * The client this position belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    /**
     * Position title (e.g., "Software Engineer", "Manager").
     */
    @Column(nullable = false, length = 100)
    private String title;

    /**
     * Unique position code within the client (e.g., "SWE1", "MGR2").
     */
    @Column(name = "position_code", nullable = false, length = 20)
    private String positionCode;

    /**
     * Detailed description of the position responsibilities.
     */
    @Column(length = 2000)
    private String description;

    /**
     * The department this position belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private DepartmentEntity department;

    /**
     * Salary range for this position.
     */
    @Embedded
    private SalaryRange salaryRange;

    /**
     * Job level/grade (1, 2, 3, etc.).
     */
    @Column(name = "job_level")
    private Integer jobLevel;

    /**
     * Job family/category (e.g., "Engineering", "Operations").
     */
    @Column(name = "job_family", length = 50)
    private String jobFamily;

    /**
     * Whether this position is exempt from overtime (FLSA).
     */
    @Column(name = "is_exempt")
    private Boolean isExempt = false;

    /**
     * Position status.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PositionStatusEnum status = PositionStatusEnum.ACTIVE;

    /**
     * Number of positions budgeted.
     */
    @Column(name = "headcount_budget")
    private Integer headcountBudget;

    /**
     * Current number of employees in this position.
     */
    @Column(name = "headcount_actual")
    private Integer headcountActual;

    /**
     * Minimum years of experience required.
     */
    @Column(name = "min_experience_years")
    private Integer minExperienceYears;

    /**
     * Required qualifications or certifications.
     */
    @Column(name = "required_qualifications", length = 1000)
    private String requiredQualifications;

    /**
     * Reports to position (for org chart).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reports_to_position_id")
    private PositionEntity reportsToPosition;

    /**
     * Soft delete timestamp.
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * User who deleted the position.
     */
    @Column(name = "deleted_by")
    private String deletedBy;

    /**
     * Check if the position has open headcount.
     */
    public boolean hasOpenHeadcount() {
        if (headcountBudget == null || headcountActual == null) {
            return true;
        }
        return headcountActual < headcountBudget;
    }
}
