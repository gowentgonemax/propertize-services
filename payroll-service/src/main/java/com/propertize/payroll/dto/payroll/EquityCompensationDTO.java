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
public class EquityCompensationDTO {
    @NotNull
    private Long employeeId;

    private List<StockGrant> stockGrants;
    private List<OptionGrant> optionGrants;
    private List<VestingEvent> vestingSchedule;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockGrant {
        private String grantType; // RSU, PERFORMANCE_SHARES, RESTRICTED_STOCK
        private LocalDate grantDate;
        @PositiveOrZero
        private Integer numberOfShares;
        @PositiveOrZero
        private BigDecimal fairMarketValue;
        private String vestingScheduleType; // CLIFF, GRADED, PERFORMANCE_BASED
        private List<VestingCondition> vestingConditions;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionGrant {
        private String optionType; // ISO, NSO, ESPP
        private LocalDate grantDate;
        private LocalDate expirationDate;
        @PositiveOrZero
        private Integer numberOfOptions;
        @PositiveOrZero
        private BigDecimal strikePrice;
        private Boolean isEarlyExerciseAllowed;
        private String vestingScheduleType;
        private List<ExerciseEvent> exerciseHistory;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VestingEvent {
        private LocalDate vestingDate;
        @PositiveOrZero
        private Integer sharesVesting;
        private String grantReference;
        private Boolean isFulfilled;
        private LocalDate fulfillmentDate;
        private String taxWithholdingMethod; // SELL_TO_COVER, CASH, SAME_DAY_SALE
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VestingCondition {
        private String conditionType; // TIME_BASED, PERFORMANCE, MILESTONE
        private String metricName;
        private String targetValue;
        private LocalDate measurementDate;
        private Boolean isAchieved;
        private String verificationDocument;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExerciseEvent {
        private LocalDate exerciseDate;
        @PositiveOrZero
        private Integer numberOfShares;
        @PositiveOrZero
        private BigDecimal exercisePrice;
        @PositiveOrZero
        private BigDecimal fairMarketValue;
        private String exerciseType; // CASH_EXERCISE, CASHLESS, SELL_TO_COVER
        private String taxWithholdingMethod;
    }
}
