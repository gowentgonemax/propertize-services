package com.propertize.payroll.entity;

import com.propertize.payroll.entity.base.BaseEntity;
import com.propertize.payroll.entity.embedded.DatePeriod;
import com.propertize.payroll.enums.PayFrequencyEnum;
import com.propertize.payroll.enums.PayPeriodStatusEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing a pay period for a client.
 */
@Entity
@Table(name = "pay_periods",
       indexes = {
           @Index(name = "idx_pay_period_client", columnList = "client_id"),
           @Index(name = "idx_pay_period_dates", columnList = "start_date, end_date"),
           @Index(name = "idx_pay_period_status", columnList = "status"),
           @Index(name = "idx_pay_period_pay_date", columnList = "pay_date")
       },
       uniqueConstraints = @UniqueConstraint(columnNames = {"client_id", "start_date", "end_date"}))
@Getter
@Setter
public class PayPeriodEntity extends BaseEntity {

    /**
     * The client this pay period belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    /**
     * The date range of this pay period.
     */
    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "startDate", column = @Column(name = "start_date", nullable = false)),
        @AttributeOverride(name = "endDate", column = @Column(name = "end_date", nullable = false))
    })
    private DatePeriod period;

    /**
     * The date when pay will be issued.
     */
    @Column(name = "pay_date", nullable = false)
    private LocalDate payDate;

    /**
     * Pay frequency for this period.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "pay_frequency", length = 20)
    private PayFrequencyEnum payFrequency;

    /**
     * Period number within the year (1-52 for weekly, 1-26 for bi-weekly, etc.).
     */
    @Column(name = "period_number")
    private Integer periodNumber;

    /**
     * The fiscal year this period belongs to.
     */
    @Column(name = "fiscal_year")
    private Integer fiscalYear;

    /**
     * Status of the pay period.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PayPeriodStatusEnum status = PayPeriodStatusEnum.OPEN;

    /**
     * Whether the pay period is locked for changes.
     */
    @Column(name = "is_locked")
    private Boolean isLocked = false;

    /**
     * Timestamp when the period was locked.
     */
    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    /**
     * User who locked the period.
     */
    @Column(name = "locked_by")
    private String lockedBy;

    /**
     * Timestamp when time entry was closed.
     */
    @Column(name = "time_entry_closed_at")
    private LocalDateTime timeEntryClosedAt;

    /**
     * Timestamp when payroll was processed.
     */
    @Column(name = "payroll_processed_at")
    private LocalDateTime payrollProcessedAt;

    /**
     * Notes about this pay period.
     */
    @Column(length = 500)
    private String notes;

    /**
     * Check if the pay period is currently active (today is within the period).
     */
    public boolean isActive() {
        LocalDate today = LocalDate.now();
        return !today.isBefore(period.getStartDate()) &&
               !today.isAfter(period.getEndDate());
    }

    /**
     * Check if time can be entered for this period.
     */
    public boolean canEnterTime() {
        return status == PayPeriodStatusEnum.OPEN && !Boolean.TRUE.equals(isLocked);
    }

    /**
     * Lock the pay period.
     */
    public void lock(String lockedByUser) {
        this.isLocked = true;
        this.lockedAt = LocalDateTime.now();
        this.lockedBy = lockedByUser;
    }

    /**
     * Close the pay period.
     */
    public void close() {
        this.status = PayPeriodStatusEnum.CLOSED;
        if (!Boolean.TRUE.equals(isLocked)) {
            lock("system");
        }
    }
}
