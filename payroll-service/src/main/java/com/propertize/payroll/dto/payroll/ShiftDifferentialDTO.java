package com.propertize.payroll.dto.payroll;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShiftDifferentialDTO {
    @NotNull
    private Long employeeId;

    private List<ShiftPremium> shiftPremiums;
    private List<SkillPremium> skillPremiums;
    private List<LocationPremium> locationPremiums;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShiftPremium {
        private String shiftName;
        private LocalTime startTime;
        private LocalTime endTime;
        @PositiveOrZero
        private BigDecimal differentialAmount;
        private String differentialType; // FLAT_AMOUNT, PERCENTAGE
        private List<String> applicableDays;
        private Boolean includeBreaks;
        private String calculationRule; // PER_HOUR, PER_SHIFT
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillPremium {
        private String skillName;
        private String certificationRequired;
        @PositiveOrZero
        private BigDecimal premiumAmount;
        private String premiumType; // FLAT_AMOUNT, PERCENTAGE
        private Boolean requiresVerification;
        private String duration; // PER_HOUR, PER_SHIFT, PERMANENT
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationPremium {
        private String locationCode;
        private String locationType; // HAZARD, REMOTE, HIGH_COST
        @PositiveOrZero
        private BigDecimal premiumAmount;
        private String premiumType; // FLAT_AMOUNT, PERCENTAGE
        private Boolean isTemporary;
        private List<String> conditions;
    }
}
