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
public class PerformanceCompensationDTO {
    @NotNull
    private Long employeeId;

    private String performanceCycle; // ANNUAL, SEMI_ANNUAL, QUARTERLY
    private LocalDate cycleStartDate;
    private LocalDate cycleEndDate;

    private List<PerformanceMetric> metrics;
    private List<PerformanceGoal> goals;
    private CompensationImpact compensationImpact;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceMetric {
        private String metricName;
        private String category; // FINANCIAL, OPERATIONAL, CUSTOMER, DEVELOPMENT
        private String measurementType; // QUANTITATIVE, QUALITATIVE
        @PositiveOrZero
        private BigDecimal weight;
        private String calculation; // ACHIEVEMENT_PERCENTAGE, SCORE_BASED, MILESTONE
        private List<MetricThreshold> thresholds;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PerformanceGoal {
        private String goalDescription;
        private String status; // NOT_STARTED, IN_PROGRESS, COMPLETED
        @PositiveOrZero
        private BigDecimal targetValue;
        @PositiveOrZero
        private BigDecimal actualValue;
        private LocalDate dueDate;
        private List<Milestone> milestones;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricThreshold {
        private String level; // BELOW_EXPECTATIONS, MEETS, EXCEEDS
        @PositiveOrZero
        private BigDecimal minimumValue;
        @PositiveOrZero
        private BigDecimal maximumValue;
        @PositiveOrZero
        private BigDecimal payoutMultiplier;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Milestone {
        private String description;
        private LocalDate targetDate;
        private String status;
        @PositiveOrZero
        private BigDecimal weightage;
        private String evidence;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompensationImpact {
        @PositiveOrZero
        private BigDecimal baseAmount;
        @PositiveOrZero
        private BigDecimal performanceMultiplier;
        private String payoutType; // CASH, EQUITY, MIXED
        private LocalDate payoutDate;
        private List<AdjustmentFactor> adjustments;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdjustmentFactor {
        private String factorType; // INDIVIDUAL, DEPARTMENT, COMPANY
        private String description;
        @PositiveOrZero
        private BigDecimal impact;
        private String applicability; // ADDITIVE, MULTIPLICATIVE
    }
}
