package com.propertize.platform.employecraft.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompensationSummary {
    private String payType;
    private BigDecimal payRate;
    private String payFrequency;
    private BigDecimal annualSalary;
}
