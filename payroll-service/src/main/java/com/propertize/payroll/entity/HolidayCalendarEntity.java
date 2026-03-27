package com.propertize.payroll.entity;

import com.propertize.payroll.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Entity representing configured holidays for a client/company.
 */
@Entity
@Table(name = "holiday_calendars", indexes = {
    @Index(name = "idx_holiday_client", columnList = "client_id"),
    @Index(name = "idx_holiday_date", columnList = "holidayDate"),
    @Index(name = "idx_holiday_year", columnList = "year")
})
@Getter
@Setter
public class HolidayCalendarEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(nullable = false, length = 100)
    private String holidayName;

    @Column(nullable = false)
    private LocalDate holidayDate;

    @Column(nullable = false)
    private Integer year;

    /**
     * Whether this is a paid holiday
     */
    @Column(nullable = false)
    private Boolean isPaid = true;

    /**
     * Whether this is a floating holiday
     */
    @Column(nullable = false)
    private Boolean isFloating = false;

    /**
     * Hours of holiday pay (typically 8)
     */
    @Column
    private Integer holidayHours = 8;

    /**
     * Whether employees working get premium pay
     */
    @Column(nullable = false)
    private Boolean premiumPayIfWorked = true;

    /**
     * Premium multiplier if worked (e.g., 1.5)
     */
    @Column
    private java.math.BigDecimal premiumMultiplier = new java.math.BigDecimal("1.50");

    /**
     * Optional description or notes
     */
    @Column(length = 500)
    private String description;

    /**
     * Whether this holiday is observed on a different date
     */
    @Column
    private LocalDate observedDate;

    /**
     * Applicable to specific departments only (null for all)
     */
    @Column(length = 100)
    private String applicableDepartment;
}
