package com.propertize.payroll.entity;

import com.propertize.payroll.entity.base.BaseEntity;
import com.propertize.payroll.enums.TimeEntryStatusEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Duration;

/**
 * Entity representing a time entry for an employee.
 */
@Entity
@Table(name = "time_entries", indexes = {
        @Index(name = "idx_time_entry_employee", columnList = "employee_id"),
        @Index(name = "idx_time_entry_date", columnList = "work_date"),
        @Index(name = "idx_time_entry_status", columnList = "status"),
        @Index(name = "idx_time_entry_timesheet", columnList = "timesheet_id")
})
@Getter
@Setter
public class TimeEntry extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private EmployeeEntity employee;

    /**
     * The timesheet this entry belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "timesheet_id")
    private TimesheetEntity timesheet;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "clock_in")
    private LocalTime clockIn;

    @Column(name = "clock_out")
    private LocalTime clockOut;

    @Column(name = "regular_hours", precision = 10, scale = 2)
    private BigDecimal regularHours = BigDecimal.ZERO;

    @Column(name = "overtime_hours", precision = 10, scale = 2)
    private BigDecimal overtimeHours = BigDecimal.ZERO;

    @Column(name = "double_time_hours", precision = 10, scale = 2)
    private BigDecimal doubleTimeHours = BigDecimal.ZERO;

    @Column(name = "break_minutes")
    private Integer breakMinutes = 0;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TimeEntryStatusEnum status = TimeEntryStatusEnum.PENDING;

    @Column(length = 500)
    private String notes;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "approved_at")
    private java.time.LocalDateTime approvedAt;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "project_code", length = 50)
    private String projectCode;

    @Column(name = "cost_center", length = 50)
    private String costCenter;

    /**
     * Calculates total hours worked based on clock in/out times.
     */
    public BigDecimal calculateTotalHours() {
        if (clockIn == null || clockOut == null) {
            return regularHours.add(overtimeHours).add(doubleTimeHours);
        }
        long minutes = Duration.between(clockIn, clockOut).toMinutes();
        minutes -= (breakMinutes != null ? breakMinutes : 0);
        return BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Approves the time entry.
     */
    public void approve(String approverUsername) {
        this.status = TimeEntryStatusEnum.APPROVED;
        this.approvedBy = approverUsername;
        this.approvedAt = java.time.LocalDateTime.now();
    }

    /**
     * Rejects the time entry.
     */
    public void reject(String rejectionNotes) {
        this.status = TimeEntryStatusEnum.REJECTED;
        this.notes = rejectionNotes;
    }
}
