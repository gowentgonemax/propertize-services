package com.propertize.payroll.entity.embedded;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Embeddable representing a salary range for a position.
 */
@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryRange implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Minimum salary for this range.
     */
    @Column(name = "min_salary", precision = 15, scale = 2)
    private BigDecimal minSalary;

    /**
     * Maximum salary for this range.
     */
    @Column(name = "max_salary", precision = 15, scale = 2)
    private BigDecimal maxSalary;

    /**
     * Target/midpoint salary for this range.
     */
    @Column(name = "target_salary", precision = 15, scale = 2)
    private BigDecimal targetSalary;

    /**
     * Currency code (e.g., USD, EUR).
     */
    @Column(name = "currency", length = 3)
    @Builder.Default
    private String currency = "USD";

    /**
     * Check if a salary is within this range.
     *
     * @param salary The salary to check
     * @return true if salary is within range, false otherwise
     */
    public boolean isWithinRange(BigDecimal salary) {
        if (salary == null) {
            return false;
        }
        boolean aboveMin = minSalary == null || salary.compareTo(minSalary) >= 0;
        boolean belowMax = maxSalary == null || salary.compareTo(maxSalary) <= 0;
        return aboveMin && belowMax;
    }

    /**
     * Calculate the position of a salary within the range as a percentage.
     *
     * @param salary The salary to calculate position for
     * @return percentage through the range (0-100), or null if range is invalid
     */
    public BigDecimal calculateRangePosition(BigDecimal salary) {
        if (salary == null || minSalary == null || maxSalary == null) {
            return null;
        }
        BigDecimal range = maxSalary.subtract(minSalary);
        if (range.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.valueOf(50); // At target if no range
        }
        BigDecimal position = salary.subtract(minSalary);
        return position.divide(range, 4, java.math.RoundingMode.HALF_UP)
                       .multiply(BigDecimal.valueOf(100));
    }
}
