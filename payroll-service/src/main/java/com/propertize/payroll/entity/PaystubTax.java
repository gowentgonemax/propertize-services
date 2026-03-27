package com.propertize.payroll.entity;

import com.propertize.payroll.entity.base.BaseEntity;
import com.propertize.payroll.enums.TaxTypeEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Entity representing a tax line item on a paystub.
 */
@Entity
@Table(name = "paystub_taxes", indexes = {
    @Index(name = "idx_paystub_tax_paystub", columnList = "paystub_id")
})
@Getter
@Setter
public class PaystubTax extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paystub_id", nullable = false)
    private Paystub paystub;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TaxTypeEnum taxType;

    @Column(length = 50)
    private String jurisdiction;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "taxable_wages", precision = 15, scale = 2)
    private BigDecimal taxableWages;

    @Column(name = "ytd_amount", precision = 15, scale = 2)
    private BigDecimal ytdAmount = BigDecimal.ZERO;

    @Column(name = "ytd_taxable_wages", precision = 15, scale = 2)
    private BigDecimal ytdTaxableWages;
}
