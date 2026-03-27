package com.propertize.payroll.entity;

import com.propertize.payroll.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entity representing individual vesting events for equity grants.
 */
@Entity
@Table(name = "vesting_events", indexes = {
    @Index(name = "idx_vesting_grant", columnList = "equity_grant_id"),
    @Index(name = "idx_vesting_date", columnList = "vestingDate"),
    @Index(name = "idx_vesting_processed", columnList = "processed")
})
@Getter
@Setter
public class VestingEventEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equity_grant_id", nullable = false)
    private EquityGrantEntity equityGrant;

    /**
     * Date of this vesting event
     */
    @Column(nullable = false)
    private LocalDate vestingDate;

    /**
     * Number of shares vesting in this event
     */
    @Column(nullable = false, precision = 15, scale = 4)
    private BigDecimal sharesVesting;

    /**
     * Fair market value on vesting date
     */
    @Column(precision = 15, scale = 4)
    private BigDecimal vestingDateFmv;

    /**
     * Taxable value of vested shares
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal taxableValue;

    /**
     * Whether this vesting event has been processed
     */
    @Column(nullable = false)
    private Boolean processed = false;

    /**
     * Date the vesting was processed
     */
    @Column
    private LocalDate processedDate;

    @Column(length = 500)
    private String notes;

    /**
     * Calculates the taxable value based on FMV and strike price.
     */
    public BigDecimal calculateTaxableValue(BigDecimal strikePrice) {
        if (vestingDateFmv == null || sharesVesting == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal priceGain = vestingDateFmv.subtract(strikePrice != null ? strikePrice : BigDecimal.ZERO);
        return priceGain.multiply(sharesVesting);
    }
}
