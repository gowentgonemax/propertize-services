package com.propertize.platform.employecraft.entity.embedded;

import com.propertize.platform.employecraft.enums.PayFrequencyEnum;
import com.propertize.platform.employecraft.enums.PayTypeEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Embeddable compensation details for Employee
 */
@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Compensation {

    @Enumerated(EnumType.STRING)
    @Column(name = "pay_type")
    private PayTypeEnum payType;

    @Column(name = "pay_rate", precision = 12, scale = 2)
    private BigDecimal payRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "pay_frequency")
    private PayFrequencyEnum payFrequency;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "bank_account_number")
    private String bankAccountNumber;

    @Column(name = "bank_routing_number")
    private String bankRoutingNumber;

    @Column(name = "overtime_eligible")
    private Boolean overtimeEligible;

    @Column(name = "overtime_rate", precision = 5, scale = 2)
    private BigDecimal overtimeRate;

    /**
     * Calculate annual salary based on pay type and frequency
     */
    public BigDecimal getAnnualSalary() {
        if (payRate == null || payType == null) {
            return BigDecimal.ZERO;
        }

        return switch (payType) {
            case SALARY -> payRate;
            case HOURLY -> payRate.multiply(BigDecimal.valueOf(2080)); // 40hrs * 52 weeks
            default -> payRate;
        };
    }
}
