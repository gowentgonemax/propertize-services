package com.propertize.payroll.entity;

import com.propertize.payroll.entity.base.BaseEntity;
import com.propertize.payroll.enums.EarningTypeEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Entity representing an earning line item on a paystub.
 */
@Entity
@Table(name = "paystub_earnings", indexes = {
    @Index(name = "idx_paystub_earning_paystub", columnList = "paystub_id")
})
@Getter
@Setter
public class PaystubEarning extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paystub_id", nullable = false)
    private Paystub paystub;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EarningTypeEnum earningType;

    @Column(precision = 10, scale = 2)
    private BigDecimal hours;

    @Column(precision = 15, scale = 4)
    private BigDecimal rate;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(length = 255)
    private String description;

    /**
     * Calculates the amount based on hours and rate if not set.
     */
    public void calculateAmount() {
        if (hours != null && rate != null) {
            this.amount = hours.multiply(rate);
        }
    }
}
