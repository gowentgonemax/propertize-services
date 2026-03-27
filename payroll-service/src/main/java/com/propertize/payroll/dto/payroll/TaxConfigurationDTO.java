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
public class TaxConfigurationDTO {
    @NotNull
    private Long employeeId;

    private String taxJurisdiction; // FEDERAL, STATE, LOCAL

    private String state;
    private String localityCode;

    private String filingStatus; // SINGLE, MARRIED_FILING_JOINTLY, MARRIED_FILING_SEPARATELY, HEAD_OF_HOUSEHOLD

    @PositiveOrZero
    private Integer allowances;

    @PositiveOrZero
    private BigDecimal additionalWithholding;

    private Boolean isExemptFromFederalWithholding;
    private Boolean isExemptFromStateWithholding;
    private Boolean isExemptFromLocalWithholding;

    private List<TaxExemption> exemptions;

    private LocalDate effectiveDate;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaxExemption {
        private String exemptionType; // FICA_MEDICARE, FICA_SOCIAL_SECURITY, STATE_UNEMPLOYMENT
        private String reason;
        private LocalDate startDate;
        private LocalDate endDate;
        private String documentationReference;
    }
}
