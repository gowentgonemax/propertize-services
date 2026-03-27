package com.propertize.payroll.dto.payroll;

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
    private String leaveType; // VACATION, SICK, PTO, BEREAVEMENT, JURY_DUTY, FMLA, MILITARY

    @FutureOrPresent
    private LocalDate startDate;

    @FutureOrPresent
    private LocalDate endDate;

    private Double totalDays;

    private Double totalHours;

    private String status; // PENDING, APPROVED, REJECTED, CANCELLED

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
