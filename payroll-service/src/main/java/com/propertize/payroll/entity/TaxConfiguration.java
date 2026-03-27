package com.propertize.payroll.entity;

import com.propertize.payroll.entity.base.BaseEntity;
import com.propertize.payroll.enums.FilingStatusEnum;
import com.propertize.payroll.enums.TaxConfigurationTypeEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entity representing tax configuration for an employee or client.
 */
@Entity
@Table(name = "tax_configurations", indexes = {
    @Index(name = "idx_tax_config_employee", columnList = "employee_id"),
    @Index(name = "idx_tax_config_client", columnList = "client_id"),
    @Index(name = "idx_tax_config_type", columnList = "tax_type")
})
@Getter
@Setter
public class TaxConfiguration extends BaseEntity {

    @Column(name = "employee_id")
    private String employeeId; // Reference to Employee in Employee Microservice

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_type", nullable = false, length = 30)
    private TaxConfigurationTypeEnum taxType;

    @Column(nullable = false, length = 50)
    private String jurisdiction;

    @Column(name = "tax_id", length = 30)
    private String taxId;

    @Enumerated(EnumType.STRING)
    @Column(name = "filing_status", length = 30)
    private FilingStatusEnum filingStatus;

    @Column
    private Integer allowances = 0;

    @Column(name = "additional_withholding", precision = 15, scale = 2)
    private BigDecimal additionalWithholding = BigDecimal.ZERO;

    @Column(name = "is_exempt")
    private Boolean isExempt = false;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "tax_rate", precision = 8, scale = 5)
    private BigDecimal taxRate;

    @Column(name = "wage_base", precision = 15, scale = 2)
    private BigDecimal wageBase;

    /**
     * Checks if this configuration is currently effective.
     */
    public boolean isEffective() {
        LocalDate today = LocalDate.now();
        return (effectiveDate == null || !today.isBefore(effectiveDate)) &&
               (endDate == null || !today.isAfter(endDate));
    }

    /**
     * Checks if this is an employee-level configuration.
     */
    public boolean isEmployeeLevel() {
        return employeeId != null;
    }

    /**
     * Checks if this is a client-level configuration.
     */
    public boolean isClientLevel() {
        return client != null && employeeId == null;
    }
}
