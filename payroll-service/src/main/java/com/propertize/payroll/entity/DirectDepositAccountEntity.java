package com.propertize.payroll.entity;

import com.propertize.payroll.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entity representing direct deposit allocations for employee pay.
 */
@Entity
@Table(name = "direct_deposit_accounts", indexes = {
    @Index(name = "idx_dd_employee", columnList = "employee_id"),
    @Index(name = "idx_dd_priority", columnList = "priority")
})
@Getter
@Setter
public class DirectDepositAccountEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private EmployeeEntity employee;

    @Column(nullable = false, length = 100)
    private String bankName;

    @Column(nullable = false, length = 9)
    private String routingNumber;

    /**
     * Account number (should be encrypted in production)
     */
    @Column(nullable = false, length = 20)
    private String accountNumber;

    /**
     * Account type (CHECKING, SAVINGS)
     */
    @Column(nullable = false, length = 20)
    private String accountType;

    /**
     * Account holder name
     */
    @Column(length = 200)
    private String accountHolderName;

    /**
     * Whether this is the primary/remainder account
     */
    @Column(nullable = false)
    private Boolean isPrimary = false;

    /**
     * Deposit amount if fixed amount
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal fixedAmount;

    /**
     * Deposit percentage if percentage-based
     */
    @Column(precision = 5, scale = 2)
    private BigDecimal percentage;

    /**
     * Allocation type (FIXED_AMOUNT, PERCENTAGE, REMAINDER)
     */
    @Column(nullable = false, length = 20)
    private String allocationType = "REMAINDER";

    /**
     * Priority order (lower = deposited first)
     */
    @Column(nullable = false)
    private Integer priority = 1;

    /**
     * Account status (ACTIVE, PRENOTE, INACTIVE)
     */
    @Column(nullable = false, length = 20)
    private String status = "PRENOTE";

    /**
     * Date prenote was sent
     */
    @Column
    private LocalDate prenoteDate;

    /**
     * Date account was verified
     */
    @Column
    private LocalDate verifiedDate;

    /**
     * Nickname for the account
     */
    @Column(length = 50)
    private String nickname;

    /**
     * Whether to mask account number in displays
     */
    @Column(nullable = false)
    private Boolean maskAccountNumber = true;

    /**
     * Returns masked account number (last 4 digits)
     */
    public String getMaskedAccountNumber() {
        if (accountNumber == null || accountNumber.length() < 4) {
            return "****";
        }
        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }
}
