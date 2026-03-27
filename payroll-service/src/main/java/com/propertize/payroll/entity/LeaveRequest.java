package com.propertize.payroll.entity;

import com.propertize.payroll.entity.base.BaseEntity;
import com.propertize.payroll.enums.LeaveStatusEnum;
import com.propertize.payroll.enums.LeaveTypeEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Entity representing a leave request from an employee.
 */
@Entity
@Table(name = "leave_requests", indexes = {
        @Index(name = "idx_leave_request_employee", columnList = "employee_id"),
        @Index(name = "idx_leave_request_status", columnList = "status"),
        @Index(name = "idx_leave_request_dates", columnList = "start_date, end_date")
})
@Getter
@Setter
public class LeaveRequest extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private EmployeeEntity employee;

    @Enumerated(EnumType.STRING)
    @Column(name = "leave_type", nullable = false, length = 30)
    private LeaveTypeEnum leaveType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "days_requested", precision = 5, scale = 2)
    private BigDecimal daysRequested;

    @Column(name = "hours_requested", precision = 6, scale = 2)
    private BigDecimal hoursRequested;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private LeaveStatusEnum status = LeaveStatusEnum.PENDING;

    @Column(length = 1000)
    private String reason;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "approved_at")
    private java.time.LocalDateTime approvedAt;

    @Column(name = "is_paid")
    private Boolean isPaid = true;

    /**
     * Calculates the number of days in this leave request.
     */
    public long calculateDays() {
        if (startDate == null || endDate == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    /**
     * Approves the leave request.
     */
    public void approve(String approverUsername) {
        this.status = LeaveStatusEnum.APPROVED;
        this.approvedBy = approverUsername;
        this.approvedAt = java.time.LocalDateTime.now();
    }

    /**
     * Rejects the leave request.
     */
    public void reject(String rejectorUsername, String reason) {
        this.status = LeaveStatusEnum.REJECTED;
        this.approvedBy = rejectorUsername;
        this.rejectionReason = reason;
        this.approvedAt = java.time.LocalDateTime.now();
    }

    /**
     * Cancels the leave request.
     */
    public void cancel() {
        this.status = LeaveStatusEnum.CANCELLED;
    }
}
