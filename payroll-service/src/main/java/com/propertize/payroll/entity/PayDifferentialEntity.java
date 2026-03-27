package com.propertize.payroll.entity;

import com.propertize.payroll.entity.base.BaseEntity;
import com.propertize.payroll.enums.DifferentialTypeEnum;
import com.propertize.payroll.enums.DifferentialStatusEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Set;

/**
 * Entity representing pay differentials (shift differentials, weekend pay,
 * etc.).
 */
@Entity
@Table(name = "pay_differentials", indexes = {
        @Index(name = "idx_differential_client", columnList = "client_id"),
        @Index(name = "idx_differential_employee", columnList = "employee_id"),
        @Index(name = "idx_differential_type", columnList = "differentialType"),
        @Index(name = "idx_differential_status", columnList = "status")
})
@Getter
@Setter
public class PayDifferentialEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private EmployeeEntity employee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DifferentialTypeEnum differentialType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DifferentialStatusEnum status = DifferentialStatusEnum.ACTIVE;

    /**
     * Differential name (e.g., "Night Shift Premium", "Weekend Pay")
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * Description of the differential
     */
    @Column(length = 500)
    private String description;

    /**
     * Fixed amount to add per hour
     */
    @Column(precision = 10, scale = 4)
    private BigDecimal flatAmount;

    /**
     * Percentage multiplier (e.g., 1.10 for 10% increase)
     */
    @Column(precision = 5, scale = 4)
    private BigDecimal percentageMultiplier;

    /**
     * Start time for time-based differentials
     */
    @Column
    private LocalTime startTime;

    /**
     * End time for time-based differentials
     */
    @Column
    private LocalTime endTime;

    /**
     * Days of week applicable (stored as comma-separated string)
     */
    @Column(length = 50)
    private String applicableDays;

    /**
     * Effective start date
     */
    @Column(nullable = false)
    private LocalDate effectiveDate;

    /**
     * Effective end date (null if ongoing)
     */
    @Column
    private LocalDate endDate;

    /**
     * Priority for applying multiple differentials (higher = applied first)
     */
    @Column
    private Integer priority = 0;

    /**
     * Whether this differential can stack with others
     */
    @Column(nullable = false)
    private Boolean isStackable = true;

    /**
     * Minimum hours worked to qualify for differential
     */
    @Column(precision = 5, scale = 2)
    private BigDecimal minimumHours;

    /**
     * Maximum hours this differential applies to
     */
    @Column(precision = 5, scale = 2)
    private BigDecimal maximumHours;

    /**
     * Calculates the differential amount for given base pay and hours.
     */
    public BigDecimal calculateDifferential(BigDecimal basePay, BigDecimal hours) {
        if (flatAmount != null && flatAmount.compareTo(BigDecimal.ZERO) > 0) {
            return flatAmount.multiply(hours);
        }
        if (percentageMultiplier != null && percentageMultiplier.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal increase = percentageMultiplier.subtract(BigDecimal.ONE);
            return basePay.multiply(increase);
        }
        return BigDecimal.ZERO;
    }

    /**
     * Checks if this differential applies to the given time.
     */
    public boolean appliesAtTime(LocalTime time) {
        if (startTime == null || endTime == null) {
            return true;
        }
        if (endTime.isAfter(startTime)) {
            return !time.isBefore(startTime) && !time.isAfter(endTime);
        } else {
            // Handles overnight shifts
            return !time.isBefore(startTime) || !time.isAfter(endTime);
        }
    }

    /**
     * Checks if this differential applies on the given day.
     */
    public boolean appliesOnDay(DayOfWeek day) {
        if (applicableDays == null || applicableDays.isEmpty()) {
            return true;
        }
        return applicableDays.toUpperCase().contains(day.name());
    }
}
