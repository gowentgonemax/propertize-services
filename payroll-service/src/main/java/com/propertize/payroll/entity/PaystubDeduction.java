package com.propertize.payroll.entity;

import com.propertize.payroll.entity.base.BaseEntity;
import com.propertize.payroll.enums.DeductionTypeEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Entity representing a deduction line item on a paystub.
 */
@Entity
@Table(name = "paystub_deductions", indexes = {
    @Index(name = "idx_paystub_deduction_paystub", columnList = "paystub_id")
})
@Getter
@Setter
public class PaystubDeduction extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paystub_id", nullable = false)
    private Paystub paystub;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DeductionTypeEnum deductionType;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "ytd_amount", precision = 15, scale = 2)
    private BigDecimal ytdAmount = BigDecimal.ZERO;

    @Column(name = "is_pre_tax")
    private Boolean isPreTax = false;

    @Column(length = 255)
    private String description;
}
