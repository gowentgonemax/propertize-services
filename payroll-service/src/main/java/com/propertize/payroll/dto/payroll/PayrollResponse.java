package com.propertize.payroll.dto.payroll;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollResponse {
    private Long payrollId;
    private LocalDate payPeriodStart;
    private LocalDate payPeriodEnd;
    private LocalDate paymentDate;
    private String payrollStatus;
    private int totalEmployees;
    private BigDecimal totalGrossPay;
    private BigDecimal totalDeductions;
    private BigDecimal totalTaxes;
    private BigDecimal totalNetPay;
    private List<PayrollEmployeeDetail> employeeDetails;
}
