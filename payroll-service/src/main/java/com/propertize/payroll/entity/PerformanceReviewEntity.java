package com.propertize.payroll.entity;

import com.propertize.payroll.entity.base.BaseEntity;
import com.propertize.payroll.enums.ReviewTypeEnum;
import com.propertize.payroll.enums.ReviewStatusEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Entity representing performance reviews that may trigger compensation changes.
 */
@Entity
@Table(name = "performance_reviews", indexes = {
    @Index(name = "idx_review_employee", columnList = "employee_id"),
    @Index(name = "idx_review_type", columnList = "reviewType"),
    @Index(name = "idx_review_status", columnList = "status"),
    @Index(name = "idx_review_date", columnList = "reviewDate")
})
@Getter
@Setter
public class PerformanceReviewEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private EmployeeEntity employee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReviewTypeEnum reviewType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReviewStatusEnum status = ReviewStatusEnum.DRAFT;

    /**
     * Date of the review
     */
    @Column(nullable = false)
    private LocalDate reviewDate;

    /**
     * Review period start
     */
    @Column
    private LocalDate periodStart;

    /**
     * Review period end
     */
    @Column
    private LocalDate periodEnd;

    /**
     * Overall rating (1-5 scale typically)
     */
    @Column(precision = 3, scale = 2)
    private BigDecimal overallRating;

    /**
     * Goals/objectives achievement rating
     */
    @Column(precision = 3, scale = 2)
    private BigDecimal goalsRating;

    /**
     * Core competencies rating
     */
    @Column(precision = 3, scale = 2)
    private BigDecimal competenciesRating;

    /**
     * Values alignment rating
     */
    @Column(precision = 3, scale = 2)
    private BigDecimal valuesRating;

    /**
     * Recommended merit increase percentage
     */
    @Column(precision = 5, scale = 4)
    private BigDecimal recommendedMeritIncrease;

    /**
     * Recommended bonus amount
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal recommendedBonus;

    /**
     * Whether promotion is recommended
     */
    @Column(nullable = false)
    private Boolean promotionRecommended = false;

    /**
     * Reviewer/manager comments
     */
    @Column(length = 2000)
    private String reviewerComments;

    /**
     * Employee self-assessment comments
     */
    @Column(length = 2000)
    private String employeeComments;

    /**
     * Goals for next review period
     */
    @Column(length = 2000)
    private String nextPeriodGoals;

    @Column(name = "reviewer_id", length = 100)
    private String reviewer;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column
    private LocalDate approvedDate;

    @Column
    private LocalDate employeeAcknowledgedDate;

    /**
     * Linked compensation change if merit increase was applied
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compensation_id")
    private CompensationEntity linkedCompensation;
}
