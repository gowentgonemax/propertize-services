package com.propertize.payroll.dto.timeentry;

import com.propertize.payroll.enums.TimeEntryStatusEnum;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeEntryDTO {

    private UUID id;

    @NotNull(message = "Employee ID is required")
    private UUID employeeId;

    private String employeeName;

    @NotNull(message = "Work date is required")
    private LocalDate workDate;

    private LocalTime clockIn;
    private LocalTime clockOut;

    private BigDecimal regularHours;
    private BigDecimal overtimeHours;
    private BigDecimal doubleTimeHours;
    private BigDecimal totalHours;

    private Integer breakMinutes;

    private TimeEntryStatusEnum status;

    private String notes;
    private String department;
    private String projectCode;
    private String costCenter;

    private UUID approvedBy;
    private String approvedByName;
    private LocalDateTime approvedAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
