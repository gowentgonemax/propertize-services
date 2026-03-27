package com.propertize.payroll.entity;

import com.propertize.payroll.entity.base.BaseEntity;
import com.propertize.payroll.enums.EarningTypeEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Entity representing earnings configuration templates.
 */
@Entity
@Table(name = "earning_codes", indexes = {
    @Index(name = "idx_earning_client", columnList = "client_id"),
    @Index(name = "idx_earning_code", columnList = "earningCode"),
    @Index(name = "idx_earning_type", columnList = "earningType")
})
@Getter
@Setter
public class EarningCodeEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(nullable = false, unique = true, length = 20)
    private String earningCode;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EarningTypeEnum earningType;

    /**
     * Whether this earning is taxable
     */
    @Column(nullable = false)
    private Boolean isTaxable = true;

    /**
     * Whether this earning is subject to FICA
     */
    @Column(nullable = false)
    private Boolean isSubjectToFica = true;

    /**
     * Whether this earning counts towards overtime base
     */
    @Column(nullable = false)
    private Boolean includeInOvertimeBase = false;

    /**
     * Whether this earning counts towards benefits calculations
     */
    @Column(nullable = false)
    private Boolean includeInBenefitsCalc = true;

    /**
     * Whether this earning counts towards retirement contributions
     */
    @Column(nullable = false)
    private Boolean includeInRetirement = true;

    /**
     * Default rate multiplier (e.g., 1.5 for overtime)
     */
    @Column(precision = 4, scale = 2)
    private BigDecimal defaultMultiplier = BigDecimal.ONE;

    /**
     * GL account code for this earning
     */
    @Column(length = 20)
    private String glAccountCode;

    /**
     * Whether this earning code is active
     */
    @Column(nullable = false)
    private Boolean isActive = true;

    /**
     * Display order in reports/screens
     */
    @Column
    private Integer displayOrder = 0;

    /**
     * W-2 Box 12 code if applicable
     */
    @Column(length = 5)
    private String w2Box12Code;
}
