package com.propertize.payroll.dto.payroll;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.FutureOrPresent;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollProcessRequest {
    @NotNull(message = "Client ID is required")
    private Long clientId;

    @NotNull(message = "Pay period start date is required")
    private LocalDate payPeriodStart;

    @NotNull(message = "Pay period end date is required")
    private LocalDate payPeriodEnd;

    @FutureOrPresent(message = "Payment date must be today or in the future")
    private LocalDate paymentDate;

    private List<Long> employeeIds; // If null, process all active employees

    private boolean isDraft; // True for preview, false for actual processing

    private String payrollType; // REGULAR, OFF_CYCLE, BONUS, COMMISSION

    private List<PayrollAdjustment> adjustments;
}
