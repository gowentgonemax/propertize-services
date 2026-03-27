package com.propertize.payroll.entity;

import com.propertize.payroll.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entity representing garnishment orders (child support, tax levies, creditor garnishments).
 */
@Entity
@Table(name = "garnishments", indexes = {
    @Index(name = "idx_garnishment_employee", columnList = "employee_id"),
    @Index(name = "idx_garnishment_status", columnList = "status"),
    @Index(name = "idx_garnishment_case", columnList = "caseNumber")
})
@Getter
@Setter
public class GarnishmentEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private EmployeeEntity employee;

    /**
     * Type of garnishment (CHILD_SUPPORT, TAX_LEVY, CREDITOR, STUDENT_LOAN, BANKRUPTCY)
     */
    @Column(nullable = false, length = 30)
    private String garnishmentType;

    /**
     * Court/agency case number
     */
    @Column(nullable = false, length = 50)
    private String caseNumber;

    /**
     * Issuing court or agency
     */
    @Column(length = 200)
    private String issuingAuthority;

    /**
     * Payee name (who receives the garnishment)
     */
    @Column(nullable = false, length = 200)
    private String payeeName;

    /**
     * Payee address
     */
    @Column(length = 500)
    private String payeeAddress;

    /**
     * Payee routing number for direct deposit
     */
    @Column(length = 9)
    private String payeeRoutingNumber;

    /**
     * Payee account number for direct deposit
     */
    @Column(length = 20)
    private String payeeAccountNumber;

    /**
     * Fixed amount per pay period
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal amountPerPayPeriod;

    /**
     * Percentage of disposable income
     */
    @Column(precision = 5, scale = 4)
    private BigDecimal percentOfDisposable;

    /**
     * Maximum percentage allowed (varies by garnishment type)
     */
    @Column(precision = 5, scale = 4)
    private BigDecimal maxPercent;

    /**
     * Total amount owed
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal totalAmountOwed;

    /**
     * Total amount paid to date
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal totalPaid = BigDecimal.ZERO;

    /**
     * Remaining balance
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal remainingBalance;

    /**
     * Order received date
     */
    @Column(nullable = false)
    private LocalDate orderReceivedDate;

    /**
     * Order effective date
     */
    @Column(nullable = false)
    private LocalDate effectiveDate;

    /**
     * Order end date (if specified)
     */
    @Column
    private LocalDate endDate;

    /**
     * Priority order for multiple garnishments
     */
    @Column(nullable = false)
    private Integer priorityOrder = 1;

    /**
     * Status (ACTIVE, SUSPENDED, COMPLETED, TERMINATED)
     */
    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    /**
     * Fees to employer per payment (if allowed)
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal processingFee;

    @Column(length = 1000)
    private String notes;

    /**
     * Reference to original order document
     */
    @Column(length = 255)
    private String orderDocumentPath;

    /**
     * Calculates remaining amount owed.
     */
    public BigDecimal getRemainingAmount() {
        if (totalAmountOwed == null) return null;
        return totalAmountOwed.subtract(totalPaid != null ? totalPaid : BigDecimal.ZERO);
    }

    /**
     * Checks if garnishment is complete.
     */
    public boolean isComplete() {
        if (totalAmountOwed == null) return false;
        return getRemainingAmount().compareTo(BigDecimal.ZERO) <= 0;
    }
}
