package com.propertize.payroll.dto.timeentry;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTimeEntryRequest {

    @NotNull(message = "Employee ID is required")
    private UUID employeeId;

    @NotNull(message = "Work date is required")
    private LocalDate workDate;

    private LocalTime clockIn;
    private LocalTime clockOut;

    private BigDecimal regularHours;
    private BigDecimal overtimeHours;
    private BigDecimal doubleTimeHours;

    private Integer breakMinutes;

    private String notes;
    private String department;
    private String projectCode;
    private String costCenter;
}
