package com.propertize.payroll.dto.payroll;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OvertimeConfigurationDTO {
    @NotNull
    private Long employeeId;

    @PositiveOrZero
    private BigDecimal regularOvertimeMultiplier; // Typically 1.5

    @PositiveOrZero
    private BigDecimal doubleTimeMultiplier; // Typically 2.0

    private Integer overtimeThresholdDaily; // Hours per day before OT
    private Integer overtimeThresholdWeekly; // Hours per week before OT

    private List<WorkWeekRule> workWeekRules;
    private List<HolidayOvertimeRule> holidayRules;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkWeekRule {
        private DayOfWeek startDay;
        private Integer regularHours;
        private Boolean isFlexibleSchedule;
        private List<String> excludedDays;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HolidayOvertimeRule {
        private String holidayName;
        private BigDecimal multiplier;
        private Boolean applyToFullDay;
        private Boolean stackWithRegularOvertime;
        private String calculationPriority;
    }
}
