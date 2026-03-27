package com.propertize.payroll.entity;

import com.propertize.payroll.entity.base.BaseEntity;
import com.propertize.payroll.enums.PayrollActionEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entity for tracking payroll run history and audit trail.
 */
@Entity
@Table(name = "payroll_history", indexes = {
    @Index(name = "idx_payroll_history_run", columnList = "payroll_run_id"),
    @Index(name = "idx_payroll_history_date", columnList = "performed_at"),
    @Index(name = "idx_payroll_history_action", columnList = "action")
})
@Getter
@Setter
public class PayrollHistoryEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payroll_run_id", nullable = false)
    private PayrollRun payrollRun;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 30)
    private PayrollActionEnum action;

    @Column(name = "performed_by", length = 100)
    private String performedBy;

    @Column(name = "performed_at", nullable = false)
    private LocalDateTime performedAt;

    @Column(length = 1000)
    private String notes;

    @Column(name = "previous_status", length = 30)
    private String previousStatus;

    @Column(name = "new_status", length = 30)
    private String newStatus;

    @Column(name = "change_data", columnDefinition = "TEXT")
    private String changeData; // JSON snapshot of changes

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @PrePersist
    public void prePersist() {
        if (performedAt == null) {
            performedAt = LocalDateTime.now();
        }
    }
}
