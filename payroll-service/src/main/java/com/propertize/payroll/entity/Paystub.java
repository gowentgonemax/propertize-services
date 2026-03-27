package com.propertize.payroll.entity;

import com.propertize.payroll.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a paystub for an employee in a payroll run.
 */
@Entity
@Table(name = "paystubs", indexes = {
        @Index(name = "idx_paystub_payroll_run", columnList = "payroll_run_id"),
        @Index(name = "idx_paystub_employee", columnList = "employee_id"),
        @Index(name = "idx_paystub_pay_date", columnList = "pay_date")
})
@Getter
@Setter
public class Paystub extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payroll_run_id", nullable = false)
    private PayrollRun payrollRun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_entity_id")
    private EmployeeEntity employee;

    @Column(name = "employee_id", nullable = false)
    private String employeeId; // Reference to Employee in Employee Microservice

    @Column(name = "employee_name")
    private String employeeName;

    @Column(name = "employee_number", length = 50)
    private String employeeNumber;

    @Column(name = "pay_date")
    private LocalDate payDate;

    @Column(name = "pay_period_start")
    private LocalDate payPeriodStart;

    @Column(name = "pay_period_end")
    private LocalDate payPeriodEnd;

    @Column(name = "check_number", length = 50)
    private String checkNumber;

    // Earnings
    @Column(name = "gross_pay", precision = 15, scale = 2)
    private BigDecimal grossPay = BigDecimal.ZERO;

    @Column(name = "gross_earnings", precision = 15, scale = 2)
    private BigDecimal grossEarnings = BigDecimal.ZERO;

    @Column(name = "net_pay", precision = 15, scale = 2)
    private BigDecimal netPay = BigDecimal.ZERO;

    @Column(name = "regular_hours", precision = 10, scale = 2)
    private BigDecimal regularHours = BigDecimal.ZERO;

    @Column(name = "overtime_hours", precision = 10, scale = 2)
    private BigDecimal overtimeHours = BigDecimal.ZERO;

    @Column(name = "double_time_hours", precision = 10, scale = 2)
    private BigDecimal doubleTimeHours = BigDecimal.ZERO;

    @Column(name = "regular_rate", precision = 15, scale = 4)
    private BigDecimal regularRate;

    @Column(name = "overtime_rate", precision = 15, scale = 4)
    private BigDecimal overtimeRate;

    // Tax amounts
    @Column(name = "federal_tax", precision = 15, scale = 2)
    private BigDecimal federalTax = BigDecimal.ZERO;

    @Column(name = "state_tax", precision = 15, scale = 2)
    private BigDecimal stateTax = BigDecimal.ZERO;

    @Column(name = "local_tax", precision = 15, scale = 2)
    private BigDecimal localTax = BigDecimal.ZERO;

    @Column(name = "social_security_tax", precision = 15, scale = 2)
    private BigDecimal socialSecurityTax = BigDecimal.ZERO;

    @Column(name = "medicare_tax", precision = 15, scale = 2)
    private BigDecimal medicareTax = BigDecimal.ZERO;

    @Column(name = "total_taxes", precision = 15, scale = 2)
    private BigDecimal totalTaxes = BigDecimal.ZERO;

    @Column(name = "total_deductions", precision = 15, scale = 2)
    private BigDecimal totalDeductions = BigDecimal.ZERO;

    // YTD amounts
    @Column(name = "ytd_gross_earnings", precision = 15, scale = 2)
    private BigDecimal ytdGrossEarnings = BigDecimal.ZERO;

    @Column(name = "ytd_net_pay", precision = 15, scale = 2)
    private BigDecimal ytdNetPay = BigDecimal.ZERO;

    @Column(name = "ytd_taxes", precision = 15, scale = 2)
    private BigDecimal ytdTaxes = BigDecimal.ZERO;

    @Column(name = "ytd_deductions", precision = 15, scale = 2)
    private BigDecimal ytdDeductions = BigDecimal.ZERO;

    @Column(name = "ytd_federal_tax", precision = 15, scale = 2)
    private BigDecimal ytdFederalTax = BigDecimal.ZERO;

    @Column(name = "ytd_state_tax", precision = 15, scale = 2)
    private BigDecimal ytdStateTax = BigDecimal.ZERO;

    @Column(name = "ytd_social_security_tax", precision = 15, scale = 2)
    private BigDecimal ytdSocialSecurityTax = BigDecimal.ZERO;

    @Column(name = "ytd_medicare_tax", precision = 15, scale = 2)
    private BigDecimal ytdMedicareTax = BigDecimal.ZERO;

    @OneToMany(mappedBy = "paystub", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PaystubEarning> earnings = new ArrayList<>();

    @OneToMany(mappedBy = "paystub", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PaystubDeduction> deductions = new ArrayList<>();

    @OneToMany(mappedBy = "paystub", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PaystubTax> taxes = new ArrayList<>();

    /**
     * Calculates and returns the total earnings amount.
     */
    public BigDecimal getTotalEarnings() {
        return earnings.stream()
                .map(PaystubEarning::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculates and returns the total deductions amount.
     */
    public BigDecimal getTotalDeductions() {
        return deductions.stream()
                .map(PaystubDeduction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculates and returns the total taxes amount.
     */
    public BigDecimal getTotalTaxes() {
        return taxes.stream()
                .map(PaystubTax::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Adds an earning to this paystub.
     */
    public void addEarning(PaystubEarning earning) {
        earnings.add(earning);
        earning.setPaystub(this);
    }

    /**
     * Adds a deduction to this paystub.
     */
    public void addDeduction(PaystubDeduction deduction) {
        deductions.add(deduction);
        deduction.setPaystub(this);
    }

    /**
     * Adds a tax to this paystub.
     */
    public void addTax(PaystubTax tax) {
        taxes.add(tax);
        tax.setPaystub(this);
    }
}
