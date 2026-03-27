package com.propertize.payroll.entity;

import com.propertize.payroll.entity.base.BaseEntity;
import com.propertize.payroll.entity.embedded.TaxInfo;
import com.propertize.payroll.enums.FilingStatusEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entity representing employee tax withholding elections (W-4 information).
 */
@Entity
@Table(name = "tax_withholdings", indexes = {
        @Index(name = "idx_withholding_employee", columnList = "employee_id"),
        @Index(name = "idx_withholding_employee_str", columnList = "employee_id_str"),
        @Index(name = "idx_withholding_tax_year", columnList = "taxYear"),
        @Index(name = "idx_withholding_current", columnList = "isCurrent"),
        @Index(name = "idx_withholding_active", columnList = "isActive")
})
@Getter
@Setter
public class TaxWithholdingEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private EmployeeEntity employee;

    /**
     * String-based employee ID for service lookups
     */
    @Column(name = "employee_id_str", length = 100)
    private String employeeId;

    /**
     * Tax year this withholding applies to
     */
    @Column(nullable = false)
    private Integer taxYear;

    /**
     * W-4 form year (2020+)
     */
    @Column(name = "w4_form_year")
    private Integer w4FormYear;

    /**
     * Embedded tax info containing W-4 data
     */
    @Embedded
    private TaxInfo taxInfo = new TaxInfo();

    /**
     * Federal filing status
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FilingStatusEnum federalFilingStatus;

    /**
     * State filing status (may differ from federal)
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private FilingStatusEnum stateFilingStatus;

    /**
     * Number of federal allowances (legacy W-4)
     */
    @Column
    private Integer federalAllowances;

    /**
     * Federal additional withholding per paycheck
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal federalAdditionalWithholding = BigDecimal.ZERO;

    /**
     * State additional withholding per paycheck
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal stateAdditionalWithholding = BigDecimal.ZERO;

    /**
     * Whether employee is exempt from federal withholding
     */
    @Column(nullable = false)
    private Boolean federalExempt = false;

    /**
     * Whether employee is exempt from state withholding
     */
    @Column(nullable = false)
    private Boolean stateExempt = false;

    /**
     * Whether employee is exempt from FICA (rare)
     */
    @Column(nullable = false)
    private Boolean ficaExempt = false;

    /**
     * Deductions to claim (new W-4)
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal deductions = BigDecimal.ZERO;

    /**
     * Whether using new W-4 (2020+) format
     */
    @Column(nullable = false)
    private Boolean isNewW4Format = true;

    /**
     * Work state for state tax purposes
     */
    @Column(length = 2)
    private String workState;

    /**
     * Resident state for state tax purposes
     */
    @Column(length = 2)
    private String residentState;

    /**
     * Date the W-4 was signed
     */
    @Column(nullable = false)
    private LocalDate effectiveDate;

    /**
     * Whether this is the current/active withholding record
     */
    @Column(nullable = false)
    private Boolean isCurrent = true;

    /**
     * Whether this withholding is active
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * End date for this withholding (when superseded by new record)
     */
    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(length = 500)
    private String notes;
}
