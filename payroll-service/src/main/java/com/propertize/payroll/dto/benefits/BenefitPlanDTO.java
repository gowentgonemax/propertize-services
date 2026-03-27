package com.propertize.payroll.dto.benefits;

import com.propertize.payroll.enums.BenefitTypeEnum;
import com.propertize.payroll.enums.PlanStatusEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class BenefitPlanDTO {

    private UUID id;

    @NotNull(message = "Client ID is required")
    private UUID clientId;

    @NotBlank(message = "Plan name is required")
    private String planName;

    @NotNull(message = "Benefit type is required")
    private BenefitTypeEnum benefitType;

    private String provider;
    private String planNumber;

    private BigDecimal employeeCost;
    private BigDecimal employerCost;
    private BigDecimal spouseCost;
    private BigDecimal childCost;
    private BigDecimal familyCost;

    private LocalDate planStartDate;
    private LocalDate planEndDate;

    private PlanStatusEnum status;

    private String description;

    private Integer enrollmentCount;
}
