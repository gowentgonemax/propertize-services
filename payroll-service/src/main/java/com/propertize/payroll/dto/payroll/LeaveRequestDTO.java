package com.propertize.payroll.dto.payroll;

import com.propertize.payroll.enums.LeaveStatusEnum;
import com.propertize.payroll.enums.LeaveTypeEnum;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.FutureOrPresent;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveRequestDTO {
    @NotNull
    private Long employeeId;

    @NotNull
    private LeaveTypeEnum leaveType;

    @FutureOrPresent
    private LocalDate startDate;

    @FutureOrPresent
    private LocalDate endDate;

    private Double totalDays;

    private Double totalHours;

    private LeaveStatusEnum status;

    private Long approverId;

    private String approverNotes;

    private Boolean isPaid;

    private String attachments; // JSON array of document references

    private String reason;

    private Boolean isHalfDay;

    private String leaveSchedule; // FULL_DAY, MORNING, AFTERNOON, CUSTOM

    private String customSchedule; // For partial day requests

    private Boolean emergencyContact;

    private String emergencyContactDetails;
}
