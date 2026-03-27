package com.propertize.payroll.entity;

import com.propertize.payroll.entity.base.BaseEntity;
import com.propertize.payroll.enums.ExportStatusEnum;
import com.propertize.payroll.enums.ExportTypeEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entity for tracking payroll exports to accounting systems.
 */
@Entity
@Table(name = "payroll_exports",
       indexes = {
           @Index(name = "idx_payroll_export_run", columnList = "payroll_run_id"),
           @Index(name = "idx_payroll_export_status", columnList = "status"),
           @Index(name = "idx_payroll_export_date", columnList = "exported_at")
       })
@Getter
@Setter
public class PayrollExportEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payroll_run_id")
    private PayrollRun payrollRun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @Enumerated(EnumType.STRING)
    @Column(name = "export_type", length = 50)
    private ExportTypeEnum exportType;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "file_url", length = 500)
    private String fileUrl;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "exported_at")
    private LocalDateTime exportedAt;

    @Column(name = "exported_by", length = 100)
    private String exportedBy;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ExportStatusEnum status = ExportStatusEnum.PENDING;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "record_count")
    private Integer recordCount;

    @Column(length = 500)
    private String notes;

    @PrePersist
    public void prePersist() {
        if (exportedAt == null) {
            exportedAt = LocalDateTime.now();
        }
    }
}
