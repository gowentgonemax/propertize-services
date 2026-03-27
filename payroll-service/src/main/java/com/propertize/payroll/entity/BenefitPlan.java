package com.propertize.payroll.entity;

import com.propertize.payroll.entity.base.BaseEntity;
import com.propertize.payroll.enums.BenefitTypeEnum;
import com.propertize.payroll.enums.PlanStatusEnum;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a benefit plan offered by a client.
 */
@Entity
@Table(name = "benefit_plans", indexes = {
    @Index(name = "idx_benefit_plan_client", columnList = "client_id"),
    @Index(name = "idx_benefit_plan_type", columnList = "benefit_type"),
    @Index(name = "idx_benefit_plan_status", columnList = "status")
})
@Getter
@Setter
public class BenefitPlan extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(nullable = false)
    private String planName;

    @Enumerated(EnumType.STRING)
    @Column(name = "benefit_type", nullable = false, length = 30)
    private BenefitTypeEnum benefitType;

    @Column
    private String provider;

    @Column(length = 50)
    private String planNumber;

    @Column(name = "employee_cost", precision = 15, scale = 2)
    private BigDecimal employeeCost;

    @Column(name = "employer_cost", precision = 15, scale = 2)
    private BigDecimal employerCost;

    @Column(name = "spouse_cost", precision = 15, scale = 2)
    private BigDecimal spouseCost;

    @Column(name = "child_cost", precision = 15, scale = 2)
    private BigDecimal childCost;

    @Column(name = "family_cost", precision = 15, scale = 2)
    private BigDecimal familyCost;

    @Column(name = "plan_start_date")
    private LocalDate planStartDate;

    @Column(name = "plan_end_date")
    private LocalDate planEndDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PlanStatusEnum status = PlanStatusEnum.ACTIVE;

    @Column(length = 1000)
    private String description;

    @OneToMany(mappedBy = "benefitPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BenefitEnrollment> enrollments = new ArrayList<>();

    /**
     * Checks if the plan is currently active.
     */
    public boolean isActive() {
        if (status != PlanStatusEnum.ACTIVE) {
            return false;
        }
        LocalDate today = LocalDate.now();
        return (planStartDate == null || !today.isBefore(planStartDate)) &&
               (planEndDate == null || !today.isAfter(planEndDate));
    }
}
