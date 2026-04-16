package com.propertize.payroll.entity;

import com.propertize.commons.enums.common.GenderEnum;
import com.propertize.commons.enums.common.RelationshipTypeEnum;
import com.propertize.commons.enums.common.RelationshipTypeEnumConverter;
import com.propertize.payroll.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Entity representing dependents covered under employee benefit enrollments.
 */
@Entity
@Table(name = "benefit_dependents", indexes = {
        @Index(name = "idx_dependent_enrollment", columnList = "enrollment_id"),
        @Index(name = "idx_dependent_ssn", columnList = "ssnLastFour")
})
@Getter
@Setter
public class BenefitDependentEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private BenefitEnrollment enrollment;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(length = 50)
    private String middleName;

    @Column(nullable = false, length = 100)
    private String lastName;

    /**
     * Relationship to employee (e.g., SPOUSE, CHILD, DOMESTIC_PARTNER)
     */
    @Convert(converter = RelationshipTypeEnumConverter.class)
    @Column(nullable = false, length = 30)
    private RelationshipTypeEnum relationship;

    @Column
    private LocalDate dateOfBirth;

    /**
     * Last 4 digits of SSN
     */
    @Column(length = 4)
    private String ssnLastFour;

    /**
     * Gender (M/F/X)
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private GenderEnum gender;

    /**
     * Whether dependent is disabled
     */
    @Column(nullable = false)
    private Boolean isDisabled = false;

    /**
     * Whether dependent is a full-time student
     */
    @Column(nullable = false)
    private Boolean isStudent = false;

    /**
     * Coverage start date
     */
    @Column(nullable = false)
    private LocalDate coverageStartDate;

    /**
     * Coverage end date (null if active)
     */
    @Column
    private LocalDate coverageEndDate;

    /**
     * Returns full name
     */
    public String getFullName() {
        StringBuilder name = new StringBuilder(firstName);
        if (middleName != null && !middleName.isEmpty()) {
            name.append(" ").append(middleName);
        }
        name.append(" ").append(lastName);
        return name.toString();
    }

    /**
     * Checks if dependent coverage is currently active
     */
    public boolean isActive() {
        LocalDate today = LocalDate.now();
        return !today.isBefore(coverageStartDate) &&
                (coverageEndDate == null || !today.isAfter(coverageEndDate));
    }
}
