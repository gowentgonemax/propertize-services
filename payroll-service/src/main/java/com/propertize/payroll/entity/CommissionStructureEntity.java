package com.propertize.payroll.entity;

import com.propertize.payroll.entity.base.BaseEntity;
import com.propertize.payroll.enums.CommissionStatusEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing commission structures for employees.
 */
@Entity
@Table(name = "commission_structures", indexes = {
    @Index(name = "idx_commission_employee", columnList = "employee_id"),
    @Index(name = "idx_commission_status", columnList = "status"),
    @Index(name = "idx_commission_effective_date", columnList = "effectiveDate")
})
@Getter
@Setter
public class CommissionStructureEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private EmployeeEntity employee;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CommissionStatusEnum status = CommissionStatusEnum.ACTIVE;

    /**
     * Base commission percentage
     */
    @Column(precision = 5, scale = 4)
    private BigDecimal baseRate;

    /**
     * Minimum sales threshold before commission applies
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal minimumThreshold;

    /**
     * Cap on total commission amount per period
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal maxCommission;

    /**
     * Whether commission is calculated on gross or net sales
     */
    @Column(nullable = false)
    private Boolean onGrossSales = true;

    /**
     * Whether commission applies to all products or specific ones
     */
    @Column(nullable = false)
    private Boolean appliesToAllProducts = true;

    @Column(nullable = false)
    private LocalDate effectiveDate;

    @Column
    private LocalDate endDate;

    @OneToMany(mappedBy = "commissionStructure", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommissionTierEntity> tiers = new ArrayList<>();

    /**
     * Calculates commission based on sales amount.
     */
    public BigDecimal calculateCommission(BigDecimal salesAmount) {
        if (salesAmount == null || salesAmount.compareTo(minimumThreshold != null ? minimumThreshold : BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }

        // If tiers exist, use tiered calculation
        if (tiers != null && !tiers.isEmpty()) {
            return calculateTieredCommission(salesAmount);
        }

        // Simple percentage calculation
        BigDecimal commission = salesAmount.multiply(baseRate != null ? baseRate : BigDecimal.ZERO);

        // Apply cap if exists
        if (maxCommission != null && commission.compareTo(maxCommission) > 0) {
            return maxCommission;
        }

        return commission;
    }

    private BigDecimal calculateTieredCommission(BigDecimal salesAmount) {
        BigDecimal totalCommission = BigDecimal.ZERO;
        BigDecimal remainingSales = salesAmount;

        List<CommissionTierEntity> sortedTiers = tiers.stream()
            .sorted((t1, t2) -> t1.getMinAmount().compareTo(t2.getMinAmount()))
            .toList();

        for (CommissionTierEntity tier : sortedTiers) {
            if (remainingSales.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal tierAmount;
            if (tier.getMaxAmount() == null || salesAmount.compareTo(tier.getMaxAmount()) <= 0) {
                tierAmount = remainingSales;
            } else {
                tierAmount = tier.getMaxAmount().subtract(tier.getMinAmount());
            }

            totalCommission = totalCommission.add(tierAmount.multiply(tier.getRate()));
            remainingSales = remainingSales.subtract(tierAmount);
        }

        return totalCommission;
    }
}
