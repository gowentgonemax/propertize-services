package com.propertize.payroll.dto.payroll;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollAdjustment {
    @NotNull
    private Long employeeId;

    private String adjustmentType; // BONUS, COMMISSION, REIMBURSEMENT, CORRECTION

    @NotNull
    private String category; // EARNINGS, DEDUCTIONS, TAXES

    private String description;

    @PositiveOrZero
    private BigDecimal amount;

    private LocalDate effectiveDate;

    private boolean isTaxable;

    private String payCode; // For accounting/GL coding

    private String approvedBy;

    private String notes;
}
