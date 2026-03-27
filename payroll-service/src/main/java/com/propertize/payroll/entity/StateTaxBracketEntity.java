package com.propertize.payroll.entity;

import com.propertize.payroll.entity.base.BaseEntity;
import com.propertize.payroll.enums.FilingStatusEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Entity representing state tax brackets for income tax calculation.
 */
@Entity
@Table(name = "state_tax_brackets", indexes = {
    @Index(name = "idx_state_bracket_year", columnList = "taxYear"),
    @Index(name = "idx_state_bracket_state", columnList = "stateCode"),
    @Index(name = "idx_state_bracket_status", columnList = "filingStatus")
})
@Getter
@Setter
public class StateTaxBracketEntity extends BaseEntity {

    /**
     * State code (e.g., "CA", "NY", "TX")
     */
    @Column(nullable = false, length = 2)
    private String stateCode;

    /**
     * Tax year this bracket applies to
     */
    @Column(nullable = false)
    private Integer taxYear;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FilingStatusEnum filingStatus;

    /**
     * Minimum income for this bracket
     */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal minIncome;

    /**
     * Maximum income for this bracket (null for top bracket)
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal maxIncome;

    /**
     * Tax rate for this bracket
     */
    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal taxRate;

    /**
     * Base tax from lower brackets
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal baseTax = BigDecimal.ZERO;

    /**
     * State-specific standard deduction
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal standardDeduction;

    /**
     * State-specific personal exemption
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal personalExemption;

    /**
     * Bracket order for processing
     */
    @Column(nullable = false)
    private Integer bracketOrder;

    /**
     * Calculates tax for income within this bracket.
     */
    public BigDecimal calculateTaxInBracket(BigDecimal taxableIncome) {
        if (taxableIncome.compareTo(minIncome) < 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal incomeInBracket;
        if (maxIncome == null) {
            incomeInBracket = taxableIncome.subtract(minIncome);
        } else {
            BigDecimal actualIncome = taxableIncome.compareTo(maxIncome) > 0 ? maxIncome : taxableIncome;
            incomeInBracket = actualIncome.subtract(minIncome);
        }

        return incomeInBracket.multiply(taxRate);
    }
}
