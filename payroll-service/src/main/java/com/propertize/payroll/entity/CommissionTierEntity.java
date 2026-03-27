package com.propertize.payroll.entity;

import com.propertize.payroll.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Entity representing tiered commission rates within a commission structure.
 */
@Entity
@Table(name = "commission_tiers", indexes = {
    @Index(name = "idx_commission_tier_structure", columnList = "commission_structure_id"),
    @Index(name = "idx_commission_tier_min", columnList = "minAmount")
})
@Getter
@Setter
public class CommissionTierEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commission_structure_id", nullable = false)
    private CommissionStructureEntity commissionStructure;

    /**
     * Tier name/description
     */
    @Column(length = 100)
    private String tierName;

    /**
     * Minimum sales amount for this tier
     */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal minAmount;

    /**
     * Maximum sales amount for this tier (null for unlimited)
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal maxAmount;

    /**
     * Commission rate for this tier (e.g., 0.05 for 5%)
     */
    @Column(nullable = false, precision = 5, scale = 4)
    private BigDecimal rate;

    /**
     * Optional flat bonus for reaching this tier
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal flatBonus;

    /**
     * Tier order/priority
     */
    @Column(nullable = false)
    private Integer tierOrder = 0;
}
