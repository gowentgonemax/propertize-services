package com.propertize.platform.employecraft.dto;

import lombok.Builder;
import java.math.BigDecimal;

@Builder
public record CompensationSummary(
        String payType,
        BigDecimal payRate,
        String payFrequency,
        BigDecimal annualSalary) {
}
