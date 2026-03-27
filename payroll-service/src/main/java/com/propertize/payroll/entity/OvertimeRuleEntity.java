package com.propertize.payroll.entity;

import com.propertize.payroll.entity.base.BaseEntity;
import com.propertize.payroll.enums.RuleStatusEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Entity representing overtime calculation rules for a client/department.
 */
@Entity
@Table(name = "overtime_rules", indexes = {
    @Index(name = "idx_ot_rule_client", columnList = "client_id"),
    @Index(name = "idx_ot_rule_status", columnList = "status")
})
@Getter
@Setter
public class OvertimeRuleEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RuleStatusEnum status = RuleStatusEnum.ACTIVE;

    /**
     * Daily hours threshold before overtime kicks in
     */
    @Column(precision = 5, scale = 2)
    private BigDecimal dailyOvertimeThreshold = new BigDecimal("8.00");

    /**
     * Weekly hours threshold before overtime kicks in
     */
    @Column(precision = 5, scale = 2)
    private BigDecimal weeklyOvertimeThreshold = new BigDecimal("40.00");

    /**
     * Daily hours threshold for double time
     */
    @Column(precision = 5, scale = 2)
    private BigDecimal dailyDoubleTimeThreshold = new BigDecimal("12.00");

    /**
     * Weekly hours threshold for double time
     */
    @Column(precision = 5, scale = 2)
    private BigDecimal weeklyDoubleTimeThreshold;

    /**
     * Overtime rate multiplier (typically 1.5)
     */
    @Column(precision = 4, scale = 2)
    private BigDecimal overtimeMultiplier = new BigDecimal("1.50");

    /**
     * Double time rate multiplier (typically 2.0)
     */
    @Column(precision = 4, scale = 2)
    private BigDecimal doubleTimeMultiplier = new BigDecimal("2.00");

    /**
     * Day of week workweek starts (for weekly OT calculation)
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private DayOfWeek workweekStartDay = DayOfWeek.MONDAY;

    /**
     * Time of day workweek starts
     */
    @Column
    private LocalTime workweekStartTime = LocalTime.MIDNIGHT;

    /**
     * Whether 7th consecutive day is double time
     */
    @Column(nullable = false)
    private Boolean seventhDayDoubleTime = false;

    /**
     * State this rule applies to (null for all states)
     */
    @Column(length = 2)
    private String applicableState;

    /**
     * Department this rule applies to (null for all departments)
     */
    @Column(length = 100)
    private String applicableDepartment;

    /**
     * Priority for rule application (higher = checked first)
     */
    @Column(nullable = false)
    private Integer priority = 0;

    /**
     * California-style daily overtime (OT after 8, DT after 12)
     */
    @Column(nullable = false)
    private Boolean useDailyOvertime = false;
}
