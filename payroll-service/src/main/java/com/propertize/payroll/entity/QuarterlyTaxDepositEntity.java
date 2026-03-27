package com.propertize.payroll.entity;

import com.propertize.payroll.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Entity representing quarterly 941 tax deposit records.
 */
@Entity
@Table(name = "quarterly_tax_deposits", indexes = {
    @Index(name = "idx_941_client", columnList = "client_id"),
    @Index(name = "idx_941_year_quarter", columnList = "taxYear, quarter")
})
@Getter
@Setter
public class QuarterlyTaxDepositEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(nullable = false)
    private Integer taxYear;

    @Column(nullable = false)
    private Integer quarter;

    /**
     * Number of employees who received wages this quarter
     */
    @Column
    private Integer employeeCount;

    /**
     * Total wages paid this quarter
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal totalWages;

    /**
     * Federal income tax withheld
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal federalIncomeTaxWithheld;

    /**
     * Taxable Social Security wages
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal taxableSocialSecurityWages;

    /**
     * Taxable Social Security tips
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal taxableSocialSecurityTips;

    /**
     * Taxable Medicare wages
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal taxableMedicareWages;

    /**
     * Total Social Security tax (employer + employee)
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal totalSocialSecurityTax;

    /**
     * Total Medicare tax (employer + employee)
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal totalMedicareTax;

    /**
     * Additional Medicare tax withheld
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal additionalMedicareTax;

    /**
     * Total taxes before adjustments
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal totalTaxesBeforeAdjustments;

    /**
     * Tax adjustments
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal adjustments;

    /**
     * Total taxes after adjustments
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal totalTaxesAfterAdjustments;

    /**
     * Total deposits made this quarter
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal totalDeposits;

    /**
     * Balance due
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal balanceDue;

    /**
     * Overpayment amount
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal overpayment;

    /**
     * Whether to apply overpayment to next return
     */
    @Column(nullable = false)
    private Boolean applyToNextReturn = true;

    /**
     * Form status (DRAFT, READY, FILED)
     */
    @Column(length = 20)
    private String formStatus = "DRAFT";

    /**
     * Date form was filed
     */
    @Column
    private java.time.LocalDate filedDate;

    /**
     * Confirmation number from filing
     */
    @Column(length = 50)
    private String confirmationNumber;

    @Column(length = 500)
    private String notes;
}
