package com.propertize.payroll.entity;

import com.propertize.commons.enums.common.FormStatusEnum;
import com.propertize.payroll.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Entity representing W-2 form data for year-end tax reporting.
 */
@Entity
@Table(name = "w2_forms", indexes = {
        @Index(name = "idx_w2_employee", columnList = "employee_id"),
        @Index(name = "idx_w2_year", columnList = "taxYear"),
        @Index(name = "idx_w2_client", columnList = "client_id")
})
@Getter
@Setter
public class W2FormEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private EmployeeEntity employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(nullable = false)
    private Integer taxYear;

    // Box 1 - Wages, tips, other compensation
    @Column(precision = 15, scale = 2)
    private BigDecimal box1Wages;

    // Box 2 - Federal income tax withheld
    @Column(precision = 15, scale = 2)
    private BigDecimal box2FederalTax;

    // Box 3 - Social Security wages
    @Column(precision = 15, scale = 2)
    private BigDecimal box3SocialSecurityWages;

    // Box 4 - Social Security tax withheld
    @Column(precision = 15, scale = 2)
    private BigDecimal box4SocialSecurityTax;

    // Box 5 - Medicare wages and tips
    @Column(precision = 15, scale = 2)
    private BigDecimal box5MedicareWages;

    // Box 6 - Medicare tax withheld
    @Column(precision = 15, scale = 2)
    private BigDecimal box6MedicareTax;

    // Box 7 - Social Security tips
    @Column(precision = 15, scale = 2)
    private BigDecimal box7SocialSecurityTips;

    // Box 8 - Allocated tips
    @Column(precision = 15, scale = 2)
    private BigDecimal box8AllocatedTips;

    // Box 10 - Dependent care benefits
    @Column(precision = 15, scale = 2)
    private BigDecimal box10DependentCareBenefits;

    // Box 11 - Nonqualified plans
    @Column(precision = 15, scale = 2)
    private BigDecimal box11NonqualifiedPlans;

    // Box 12 codes and amounts (stored as JSON or separate fields)
    @Column(length = 5)
    private String box12aCode;
    @Column(precision = 15, scale = 2)
    private BigDecimal box12aAmount;

    @Column(length = 5)
    private String box12bCode;
    @Column(precision = 15, scale = 2)
    private BigDecimal box12bAmount;

    @Column(length = 5)
    private String box12cCode;
    @Column(precision = 15, scale = 2)
    private BigDecimal box12cAmount;

    @Column(length = 5)
    private String box12dCode;
    @Column(precision = 15, scale = 2)
    private BigDecimal box12dAmount;

    // Box 13 checkboxes
    @Column(nullable = false)
    private Boolean box13StatutoryEmployee = false;
    @Column(nullable = false)
    private Boolean box13RetirementPlan = false;
    @Column(nullable = false)
    private Boolean box13ThirdPartySickPay = false;

    // Box 14 - Other
    @Column(length = 500)
    private String box14Other;

    // State tax information (boxes 15-20)
    @Column(length = 2)
    private String state1Code;
    @Column(length = 20)
    private String state1EmployerId;
    @Column(precision = 15, scale = 2)
    private BigDecimal state1Wages;
    @Column(precision = 15, scale = 2)
    private BigDecimal state1Tax;
    @Column(precision = 15, scale = 2)
    private BigDecimal local1Wages;
    @Column(precision = 15, scale = 2)
    private BigDecimal local1Tax;
    @Column(length = 50)
    private String local1Name;

    // Second state (if applicable)
    @Column(length = 2)
    private String state2Code;
    @Column(length = 20)
    private String state2EmployerId;
    @Column(precision = 15, scale = 2)
    private BigDecimal state2Wages;
    @Column(precision = 15, scale = 2)
    private BigDecimal state2Tax;

    /**
     * Form status
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private FormStatusEnum formStatus = FormStatusEnum.DRAFT;

    /**
     * Date form was finalized
     */
    @Column
    private java.time.LocalDate finalizedDate;

    /**
     * Date form was distributed to employee
     */
    @Column
    private java.time.LocalDate distributedDate;

    /**
     * Whether corrections were filed (W-2c)
     */
    @Column(nullable = false)
    private Boolean correctionFiled = false;
}
