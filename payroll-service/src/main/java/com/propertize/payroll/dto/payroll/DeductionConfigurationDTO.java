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
public class DeductionConfigurationDTO {
    @NotNull
    private Long employeeId;

    private List<BenefitDeduction> benefitDeductions;
    private List<RetirementDeduction> retirementDeductions;
    private List<GarnishmentDeduction> garnishments;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BenefitDeduction {
        private String benefitType; // HEALTH, DENTAL, VISION, LIFE, DISABILITY
        private String planCode;
        private String coverageLevel; // EMPLOYEE_ONLY, EMPLOYEE_SPOUSE, FAMILY
        @PositiveOrZero
        private BigDecimal employeeContribution;
        @PositiveOrZero
        private BigDecimal employerContribution;
        private String frequency; // PER_PAY_PERIOD, MONTHLY, ANNUALLY
        private LocalDate effectiveDate;
        private LocalDate endDate;
        private Boolean isCafeteriaPlan;
        private Boolean isPreTax;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RetirementDeduction {
        private String planType; // 401K, 403B, IRA, ROTH_401K
        @PositiveOrZero
        private BigDecimal employeeContributionPercent;
        @PositiveOrZero
        private BigDecimal employeeContributionAmount;
        @PositiveOrZero
        private BigDecimal employerMatchPercent;
        @PositiveOrZero
        private BigDecimal employerMatchLimit;
        private Boolean isRoth;
        private Boolean isCatchUpEnabled;
        private LocalDate effectiveDate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GarnishmentDeduction {
        private String garnishmentType; // CHILD_SUPPORT, TAX_LEVY, WAGE_ASSIGNMENT
        private String caseNumber;
        @PositiveOrZero
        private BigDecimal amount;
        private String calculation; // FLAT_AMOUNT, PERCENTAGE
        @PositiveOrZero
        private BigDecimal maxPercentage;
        private String frequency;
        private LocalDate startDate;
        private LocalDate endDate;
        private String priority;
        private String recipientInfo;
    }
}
