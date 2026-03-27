package com.propertize.payroll.entity;

import com.propertize.payroll.entity.base.BaseEntity;
import com.propertize.payroll.enums.TaxTypeEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Entity representing individual tax calculations on a paystub.
 * Provides detailed breakdown of each tax type.
 */
@Entity
@Table(name = "tax_calculations", indexes = {
    @Index(name = "idx_tax_calc_paystub", columnList = "paystub_id"),
    @Index(name = "idx_tax_calc_type", columnList = "taxType")
})
@Getter
@Setter
public class TaxCalculationEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paystub_id", nullable = false)
    private Paystub paystub;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TaxTypeEnum taxType;

    /**
     * Tax jurisdiction (e.g., "US", "CA", "NY")
     */
    @Column(nullable = false, length = 10)
    private String jurisdiction;

    /**
     * Taxable wages for this tax type
     */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal taxableWages;

    /**
     * Pre-tax deductions that reduce taxable wages
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal preTaxDeductions = BigDecimal.ZERO;

    /**
     * Tax-exempt wages (if any)
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal exemptWages = BigDecimal.ZERO;

    /**
     * Tax rate applied
     */
    @Column(precision = 7, scale = 6)
    private BigDecimal taxRate;

    /**
     * Calculated tax amount
     */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal calculatedTax;

    /**
     * Additional withholding amount
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal additionalWithholding = BigDecimal.ZERO;

    /**
     * Final tax amount after adjustments
     */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal finalTaxAmount;

    /**
     * YTD taxable wages
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal ytdTaxableWages;

    /**
     * YTD tax withheld
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal ytdTaxWithheld;

    /**
     * Whether wage base limit has been reached (for SS, etc.)
     */
    @Column(nullable = false)
    private Boolean wageBaseLimitReached = false;

    /**
     * Reference to the tax configuration used
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tax_config_id")
    private TaxConfiguration taxConfiguration;
}
