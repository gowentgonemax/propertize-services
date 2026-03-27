package com.propertize.payroll.entity.embedded;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Embeddable value object representing a date period with start and end dates.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class DatePeriod {

    @Column(name = "period_start_date")
    private LocalDate startDate;

    @Column(name = "period_end_date")
    private LocalDate endDate;

    /**
     * Returns the number of days in this period (inclusive).
     */
    public long getDaysInPeriod() {
        if (startDate == null || endDate == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    /**
     * Checks if a given date falls within this period.
     */
    public boolean contains(LocalDate date) {
        if (startDate == null || endDate == null || date == null) {
            return false;
        }
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    /**
     * Checks if this period overlaps with another period.
     */
    public boolean overlaps(DatePeriod other) {
        if (startDate == null || endDate == null || other == null ||
            other.startDate == null || other.endDate == null) {
            return false;
        }
        return !this.endDate.isBefore(other.startDate) && !this.startDate.isAfter(other.endDate);
    }

    /**
     * Checks if the period is currently active (current date is within the period).
     */
    public boolean isActive() {
        return contains(LocalDate.now());
    }

    /**
     * Checks if the period has ended.
     */
    public boolean hasEnded() {
        return endDate != null && LocalDate.now().isAfter(endDate);
    }

    /**
     * Checks if the period has not started yet.
     */
    public boolean hasNotStarted() {
        return startDate != null && LocalDate.now().isBefore(startDate);
    }
}
