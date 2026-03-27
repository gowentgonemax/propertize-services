package com.propertize.payroll.dto.timesheet.response;

import com.propertize.payroll.dto.timeentry.TimeEntryDTO;
import com.propertize.payroll.enums.TimesheetStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimesheetResponse {
    private UUID id;
    private String employeeId;
    private String employeeName;
    private UUID payPeriodId;
    private LocalDate weekStartDate;
    private LocalDate weekEndDate;
    private List<TimeEntryDTO> timeEntries;
    private BigDecimal totalRegularHours;
    private BigDecimal totalOvertimeHours;
    private BigDecimal totalDoubleTimeHours;
    private BigDecimal totalPtoHours;
    private BigDecimal totalSickHours;
    private BigDecimal totalHolidayHours;
    private BigDecimal totalHours;
    private TimesheetStatusEnum status;
    private LocalDateTime submittedAt;
    private String approvedByName;
    private LocalDateTime approvedAt;
    private String rejectionReason;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
