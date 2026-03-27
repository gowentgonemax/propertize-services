package com.propertize.payroll.entity;

import com.propertize.payroll.entity.base.BaseEntity;
import com.propertize.payroll.enums.EquityTypeEnum;
import com.propertize.payroll.enums.GrantStatusEnum;
import com.propertize.payroll.enums.VestingScheduleEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing equity compensation (stock options, RSUs, etc.).
 */
@Entity
@Table(name = "equity_grants", indexes = {
    @Index(name = "idx_equity_employee", columnList = "employee_id"),
    @Index(name = "idx_equity_type", columnList = "equityType"),
    @Index(name = "idx_equity_status", columnList = "status"),
    @Index(name = "idx_equity_grant_date", columnList = "grantDate")
})
@Getter
@Setter
public class EquityGrantEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private EmployeeEntity employee;

    @Column(nullable = false, unique = true, length = 50)
    private String grantNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EquityTypeEnum equityType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GrantStatusEnum status = GrantStatusEnum.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private VestingScheduleEnum vestingSchedule;

    /**
     * Total shares/units granted
     */
    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal totalShares;

    /**
     * Shares currently vested
     */
    @Column(precision = 15, scale = 4)
    private BigDecimal vestedShares = BigDecimal.ZERO;

    /**
     * Shares exercised (for options)
     */
    @Column(precision = 15, scale = 4)
    private BigDecimal exercisedShares = BigDecimal.ZERO;

    /**
     * Shares forfeited
     */
    @Column(precision = 15, scale = 4)
    private BigDecimal forfeitedShares = BigDecimal.ZERO;

    /**
     * Strike/exercise price per share (for options)
     */
    @Column(precision = 15, scale = 4)
    private BigDecimal strikePrice;

    /**
     * Grant date fair market value
     */
    @Column(precision = 15, scale = 4)
    private BigDecimal grantDateFmv;

    /**
     * Date the grant was awarded
     */
    @Column(nullable = false)
    private LocalDate grantDate;

    /**
     * Date vesting begins (may differ from grant date)
     */
    @Column(nullable = false)
    private LocalDate vestingStartDate;

    /**
     * Cliff date (first vesting event)
     */
    @Column
    private LocalDate cliffDate;

    /**
     * Expiration date (for options)
     */
    @Column
    private LocalDate expirationDate;

    /**
     * Total vesting period in months
     */
    @Column
    private Integer vestingPeriodMonths;

    /**
     * Cliff period in months
     */
    @Column
    private Integer cliffPeriodMonths;

    /**
     * Percentage that vests at cliff
     */
    @Column(precision = 5, scale = 4)
    private BigDecimal cliffPercentage;

    /**
     * Monthly vesting percentage after cliff
     */
    @Column(precision = 5, scale = 4)
    private BigDecimal monthlyVestingPercentage;

    @Column(length = 500)
    private String notes;

    @OneToMany(mappedBy = "equityGrant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VestingEventEntity> vestingEvents = new ArrayList<>();

    /**
     * Calculates unvested shares.
     */
    public BigDecimal getUnvestedShares() {
        return totalShares.subtract(vestedShares).subtract(forfeitedShares);
    }

    /**
     * Calculates exercisable shares.
     */
    public BigDecimal getExercisableShares() {
        return vestedShares.subtract(exercisedShares);
    }

    /**
     * Checks if the grant is fully vested.
     */
    public boolean isFullyVested() {
        return getUnvestedShares().compareTo(BigDecimal.ZERO) <= 0;
    }

    /**
     * Calculates the current value of unvested shares.
     */
    public BigDecimal getUnvestedValue(BigDecimal currentPrice) {
        if (currentPrice == null) return BigDecimal.ZERO;
        return getUnvestedShares().multiply(currentPrice.subtract(strikePrice != null ? strikePrice : BigDecimal.ZERO));
    }
}
