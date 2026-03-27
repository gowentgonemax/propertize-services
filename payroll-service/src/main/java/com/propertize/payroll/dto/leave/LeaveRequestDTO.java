package com.propertize.payroll.dto.leave;

import com.propertize.payroll.enums.LeaveStatusEnum;
import com.propertize.payroll.enums.LeaveTypeEnum;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveRequestDTO {

    private UUID id;

    @NotNull(message = "Employee ID is required")
    private UUID employeeId;

    private String employeeName;

    @NotNull(message = "Leave type is required")
    private LeaveTypeEnum leaveType;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    private BigDecimal daysRequested;
    private BigDecimal hoursRequested;

    private LeaveStatusEnum status;

    private String reason;
    private String rejectionReason;

    private UUID approvedBy;
    private String approvedByName;
    private LocalDateTime approvedAt;

    private Boolean isPaid;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
