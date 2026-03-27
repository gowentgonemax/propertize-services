package com.propertize.payroll.entity.embedded;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Embeddable value object representing employment information.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class EmploymentInfo {

    @Column(name = "hire_date")
    private LocalDate hireDate;

    @Column(name = "termination_date")
    private LocalDate terminationDate;

    @Column(name = "original_hire_date")
    private LocalDate originalHireDate;

    @Column(name = "seniority_date")
    private LocalDate seniorityDate;

    @Column(name = "last_raise_date")
    private LocalDate lastRaiseDate;

    @Column(name = "last_review_date")
    private LocalDate lastReviewDate;

    @Column(name = "probation_end_date")
    private LocalDate probationEndDate;

    /**
     * Checks if employee is in probation period.
     */
    public boolean isInProbation() {
        if (probationEndDate == null) return false;
        return LocalDate.now().isBefore(probationEndDate);
    }

    /**
     * Calculates years of service.
     */
    public int getYearsOfService() {
        LocalDate start = seniorityDate != null ? seniorityDate : hireDate;
        if (start == null) return 0;
        LocalDate end = terminationDate != null ? terminationDate : LocalDate.now();
        return (int) java.time.temporal.ChronoUnit.YEARS.between(start, end);
    }
}
