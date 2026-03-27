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
public class CommissionStructureDTO {
    @NotNull
    private Long employeeId;

    private String commissionPlanType; // TIERED, FLAT_RATE, GRADUATED, BLENDED
    private LocalDate effectiveDate;
    private LocalDate expirationDate;
    private String paymentFrequency; // MONTHLY, QUARTERLY, ANNUAL

    private List<CommissionTier> tiers;
    private List<ProductCommission> productRates;
    private QuotaDetails quota;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CommissionTier {
        @PositiveOrZero
        private BigDecimal lowerBound;
        private BigDecimal upperBound; // null means unlimited
        @PositiveOrZero
        private BigDecimal commissionRate;
        private String rateType; // PERCENTAGE, FLAT_AMOUNT
        private Boolean isAccelerator;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductCommission {
        private String productCode;
        private String productCategory;
        @PositiveOrZero
        private BigDecimal commissionRate;
        private String rateType; // PERCENTAGE, FLAT_AMOUNT
        private Boolean includeDiscounts;
        private List<String> excludedPromotions;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuotaDetails {
        @PositiveOrZero
        private BigDecimal targetAmount;
        private String quotaPeriod; // MONTHLY, QUARTERLY, ANNUAL
        private Boolean isProrated;
        @PositiveOrZero
        private BigDecimal minimumThreshold;
        private List<QuotaAdjustment> adjustments;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuotaAdjustment {
        private LocalDate effectiveDate;
        private String adjustmentType; // INCREASE, DECREASE, TERRITORY_CHANGE
        @PositiveOrZero
        private BigDecimal adjustmentAmount;
        private String reason;
        private String approvedBy;
    }
}
