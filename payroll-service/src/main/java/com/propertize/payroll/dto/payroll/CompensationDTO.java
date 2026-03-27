package com.propertize.payroll.dto.payroll;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompensationDTO {
    @NotNull
    private Long employeeId;

    private String compensationType; // SALARY, HOURLY, COMMISSION, HYBRID

    @PositiveOrZero
    private BigDecimal basePayRate;

    private String payFrequency; // WEEKLY, BI_WEEKLY, SEMI_MONTHLY, MONTHLY

    private LocalDate effectiveDate;

    private String currency;

    private Boolean isExempt;

    // Overtime Configuration
    private BigDecimal overtimeRate;
    private BigDecimal doubleTimeRate;
    private Integer overtimeThreshold;

    // Commission Structure
    private String commissionPlan;
    private BigDecimal commissionRate;
    private BigDecimal quotaAmount;
    private String commissionFrequency;

    // Bonus Configuration
    private List<BonusStructure> bonusPlans;

    // Pay Grade Information
    private String payGrade;
    private String payBand;
    private BigDecimal minimumSalary;
    private BigDecimal midpointSalary;
    private BigDecimal maximumSalary;

    // Additional Compensation
    private List<AdditionalPay> additionalPay;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BonusStructure {
        private String bonusType; // PERFORMANCE, RETENTION, SIGNING, SPOT
        private BigDecimal amount;
        private String frequency;
        private String criteria;
        private LocalDate targetDate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdditionalPay {
        private String type; // SHIFT_DIFFERENTIAL, HAZARD, ON_CALL, LANGUAGE
        private BigDecimal amount;
        private String calculation; // FLAT, PERCENTAGE
        private String frequency;
        private String conditions;
    }
}
