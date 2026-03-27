package com.propertize.payroll.dto.leave;

import com.propertize.payroll.enums.LeaveTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveBalanceDTO {

    private UUID id;
    private UUID employeeId;
    private String employeeName;

    private LeaveTypeEnum leaveType;
    private Integer year;

    private BigDecimal beginningBalance;
    private BigDecimal accruedHours;
    private BigDecimal usedHours;
    private BigDecimal adjustedHours;
    private BigDecimal carriedOverHours;

    private BigDecimal availableBalance;

    private BigDecimal annualAccrualCap;
    private BigDecimal carryoverCap;
    private BigDecimal accrualRatePerPeriod;

    private Boolean allowNegativeBalance;
    private BigDecimal maxNegativeBalance;

    private LocalDate lastAccrualDate;
}
