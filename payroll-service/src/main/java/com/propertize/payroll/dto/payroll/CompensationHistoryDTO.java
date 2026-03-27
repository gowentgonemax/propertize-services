package com.propertize.payroll.dto.payroll;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompensationHistoryDTO {
    @NotNull
    private Long employeeId;

    private List<CompensationChange> compensationChanges;
    private List<PromotionHistory> promotions;
    private List<BonusHistory> bonuses;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompensationChange {
        private LocalDateTime effectiveDate;
        private String changeType; // MERIT, MARKET_ADJUSTMENT, PROMOTION, TRANSFER
        @PositiveOrZero
        private BigDecimal oldBase;
        @PositiveOrZero
        private BigDecimal newBase;
        private String reason;
        private String approvedBy;
        private LocalDateTime approvalDate;
        private List<CompensationComponent> components;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompensationComponent {
        private String componentType; // BASE_PAY, BONUS, COMMISSION, ALLOWANCE
        private BigDecimal amount;
        private String frequency;
        private String currency;
        private Boolean isProrated;
        private String paymentSchedule;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PromotionHistory {
        private LocalDateTime effectiveDate;
        private String oldTitle;
        private String newTitle;
        private String oldDepartment;
        private String newDepartment;
        private String oldGrade;
        private String newGrade;
        private BigDecimal compensationImpact;
        private String promotionType; // LATERAL, UPWARD, DEPARTMENT_CHANGE
        private String approvedBy;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BonusHistory {
        private LocalDateTime awardDate;
        private String bonusType; // PERFORMANCE, SPOT, RETENTION, SIGNING
        private BigDecimal amount;
        private String currency;
        private String paymentSchedule; // IMMEDIATE, INSTALLMENTS
        private Boolean isGuaranteed;
        private String performancePeriod;
        private List<BonusInstallment> installments;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BonusInstallment {
        private LocalDateTime scheduledDate;
        private BigDecimal amount;
        private String status; // SCHEDULED, PAID, CANCELLED
        private LocalDateTime actualPaymentDate;
        private String payrollRunReference;
    }
}
