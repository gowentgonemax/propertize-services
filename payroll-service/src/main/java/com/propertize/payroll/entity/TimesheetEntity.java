package com.propertize.payroll.entity;

import com.propertize.payroll.entity.base.BaseEntity;
import com.propertize.payroll.entity.embedded.DatePeriod;
import com.propertize.payroll.enums.TimesheetStatusEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a timesheet containing time entries for an employee.
 */
@Entity
@Table(name = "timesheets", indexes = {
        @Index(name = "idx_timesheet_employee", columnList = "employee_id"),
        @Index(name = "idx_timesheet_pay_period", columnList = "pay_period_id"),
        @Index(name = "idx_timesheet_status", columnList = "status"),
        @Index(name = "idx_timesheet_dates", columnList = "week_start_date, week_end_date")
}, uniqueConstraints = @UniqueConstraint(columnNames = { "employee_id", "pay_period_id", "week_start_date" }))
@Getter
@Setter
public class TimesheetEntity extends BaseEntity {

    /**
     * Reference to the employee (from Employee microservice).
     */
    @Column(name = "employee_id", nullable = false)
    private String employeeId;

    /**
     * The pay period this timesheet belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pay_period_id")
    private PayPeriodEntity payPeriod;

    /**
     * The week period covered by this timesheet.
     */
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "startDate", column = @Column(name = "week_start_date")),
            @AttributeOverride(name = "endDate", column = @Column(name = "week_end_date"))
    })
    private DatePeriod weekPeriod;

    /**
     * Time entries associated with this timesheet.
     */
    @OneToMany(mappedBy = "timesheet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TimeEntry> timeEntries = new ArrayList<>();

    /**
     * Total regular hours worked.
     */
    @Column(name = "total_regular_hours", precision = 10, scale = 2)
    private BigDecimal totalRegularHours = BigDecimal.ZERO;

    /**
     * Total overtime hours worked (1.5x).
     */
    @Column(name = "total_overtime_hours", precision = 10, scale = 2)
    private BigDecimal totalOvertimeHours = BigDecimal.ZERO;

    /**
     * Total double-time hours worked (2x).
     */
    @Column(name = "total_double_time_hours", precision = 10, scale = 2)
    private BigDecimal totalDoubleTimeHours = BigDecimal.ZERO;

    /**
     * Total PTO/vacation hours.
     */
    @Column(name = "total_pto_hours", precision = 10, scale = 2)
    private BigDecimal totalPtoHours = BigDecimal.ZERO;

    /**
     * Total sick hours.
     */
    @Column(name = "total_sick_hours", precision = 10, scale = 2)
    private BigDecimal totalSickHours = BigDecimal.ZERO;

    /**
     * Total holiday hours.
     */
    @Column(name = "total_holiday_hours", precision = 10, scale = 2)
    private BigDecimal totalHolidayHours = BigDecimal.ZERO;

    /**
     * Timesheet status.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TimesheetStatusEnum status = TimesheetStatusEnum.DRAFT;

    /**
     * Timestamp when submitted for approval.
     */
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    /**
     * User who approved the timesheet.
     */
    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    /**
     * Timestamp when approved.
     */
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    /**
     * User who rejected the timesheet.
     */
    @Column(name = "rejected_by", length = 100)
    private String rejectedBy;

    /**
     * Timestamp when rejected.
     */
    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    /**
     * Reason for rejection if applicable.
     */
    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    /**
     * Employee notes.
     */
    @Column(length = 1000)
    private String notes;

    /**
     * Manager/approver notes.
     */
    @Column(name = "approver_notes", length = 1000)
    private String approverNotes;

    /**
     * Calculate all totals from time entries.
     */
    public void calculateTotals() {
        this.totalRegularHours = timeEntries.stream()
                .map(TimeEntry::getRegularHours)
                .filter(h -> h != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.totalOvertimeHours = timeEntries.stream()
                .map(TimeEntry::getOvertimeHours)
                .filter(h -> h != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.totalDoubleTimeHours = timeEntries.stream()
                .map(TimeEntry::getDoubleTimeHours)
                .filter(h -> h != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Submit the timesheet for approval.
     */
    public void submit() {
        if (this.status != TimesheetStatusEnum.DRAFT) {
            throw new IllegalStateException("Only DRAFT timesheets can be submitted");
        }
        calculateTotals();
        this.status = TimesheetStatusEnum.SUBMITTED;
        this.submittedAt = LocalDateTime.now();
    }

    /**
     * Approve the timesheet.
     */
    public void approve(String approverUsername) {
        if (this.status != TimesheetStatusEnum.SUBMITTED) {
            throw new IllegalStateException("Only SUBMITTED timesheets can be approved");
        }
        this.status = TimesheetStatusEnum.APPROVED;
        this.approvedBy = approverUsername;
        this.approvedAt = LocalDateTime.now();
    }

    /**
     * Reject the timesheet.
     */
    public void reject(String rejectorUsername, String reason) {
        if (this.status != TimesheetStatusEnum.SUBMITTED) {
            throw new IllegalStateException("Only SUBMITTED timesheets can be rejected");
        }
        this.status = TimesheetStatusEnum.REJECTED;
        this.rejectedBy = rejectorUsername;
        this.rejectedAt = LocalDateTime.now();
        this.rejectionReason = reason;
    }

    /**
     * Return to draft status (after rejection).
     */
    public void returnToDraft() {
        this.status = TimesheetStatusEnum.DRAFT;
    }

    /**
     * Get total hours worked.
     */
    public BigDecimal getTotalHours() {
        return totalRegularHours
                .add(totalOvertimeHours)
                .add(totalDoubleTimeHours);
    }

    /**
     * Add a time entry to this timesheet.
     */
    public void addTimeEntry(TimeEntry entry) {
        timeEntries.add(entry);
        entry.setTimesheet(this);
    }

    /**
     * Remove a time entry from this timesheet.
     */
    public void removeTimeEntry(TimeEntry entry) {
        timeEntries.remove(entry);
        entry.setTimesheet(null);
    }
}
