package com.propertize.payroll.dto.payroll;

import com.propertize.payroll.enums.TimeEntryStatusEnum;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeEntryDTO {
    @NotNull
    private Long employeeId;

    @NotNull
    private LocalDateTime startTime;

    @NotNull
    private LocalDateTime endTime;

    private String entryType; // REGULAR, OVERTIME, PTO, SICK, VACATION

    private String projectCode;

    private String departmentCode;

    private String locationCode;

    private BigDecimal hoursWorked;

    private TimeEntryStatusEnum status;

    private String approverNotes;

    private Boolean isOnCall;

    private Boolean isHoliday;

    private String shiftCode;

    private String breakDetails; // JSON string containing break information

    private String gpsLocation; // For mobile clock-ins
}
