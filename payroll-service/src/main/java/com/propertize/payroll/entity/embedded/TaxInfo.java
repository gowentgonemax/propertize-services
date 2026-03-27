package com.propertize.payroll.entity.embedded;

import com.propertize.payroll.enums.FilingStatusEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Embedded value object for tax withholding information (W-4 data).
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaxInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Federal filing status
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "filing_status", length = 30)
    private FilingStatusEnum filingStatus;

    /**
     * Whether employee has claimed exempt status
     */
    @Column(name = "is_exempt")
    private Boolean isExempt = false;

    /**
     * Number of allowances (legacy W-4)
     */
    @Column(name = "allowances")
    private Integer allowances = 0;

    /**
     * Additional withholding per paycheck
     */
    @Column(name = "additional_withholding", precision = 10, scale = 2)
    private BigDecimal additionalWithholding = BigDecimal.ZERO;

    /**
     * Other income to include in calculations (new W-4 Step 4a)
     */
    @Column(name = "other_income", precision = 15, scale = 2)
    private BigDecimal otherIncome = BigDecimal.ZERO;

    /**
     * Deductions to claim (new W-4 Step 4b)
     */
    @Column(name = "deductions_claimed", precision = 15, scale = 2)
    private BigDecimal deductions = BigDecimal.ZERO;

    /**
     * Dependent credit (new W-4 Step 3)
     */
    @Column(name = "dependent_credit", precision = 10, scale = 2)
    private BigDecimal dependentCredit = BigDecimal.ZERO;

    /**
     * Whether employee has multiple jobs or spouse works (new W-4 Step 2)
     */
    @Column(name = "multiple_jobs")
    private Boolean multipleJobs = false;
}
