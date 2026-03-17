package com.propertize.platform.auth.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Configuration for time-based access control.
 *
 * Defines business hours and provides utility methods to determine
 * whether the current time falls within permitted access windows.
 * Used in conjunction with temporal permissions to enforce
 * time-based access policies.
 *
 * <p>
 * Business hours are defined as 9:00 AM to 6:00 PM on weekdays
 * (Monday through Friday).
 * </p>
 *
 * @version 1.0 - Phase 1: Time-Based Access Control
 */
@Configuration
@EnableScheduling
@Slf4j
public class TimeBasedAccessConfig {

    /** Start of business hours (9:00 AM). */
    public static final LocalTime BUSINESS_HOURS_START = LocalTime.of(9, 0);

    /** End of business hours (6:00 PM). */
    public static final LocalTime BUSINESS_HOURS_END = LocalTime.of(18, 0);

    /** First working day of the week. */
    public static final DayOfWeek WORK_WEEK_START = DayOfWeek.MONDAY;

    /** Last working day of the week. */
    public static final DayOfWeek WORK_WEEK_END = DayOfWeek.FRIDAY;

    /**
     * Check if the current time is within business hours (9 AM - 6 PM).
     *
     * @return true if the current time is between {@link #BUSINESS_HOURS_START}
     *         and {@link #BUSINESS_HOURS_END}
     */
    public static boolean isWithinBusinessHours() {
        LocalTime now = LocalTime.now();
        boolean within = !now.isBefore(BUSINESS_HOURS_START) && now.isBefore(BUSINESS_HOURS_END);
        log.debug("Business hours check: current={}, within={}", now, within);
        return within;
    }

    /**
     * Check if the given timestamp falls within business hours.
     *
     * @param dateTime the timestamp to check
     * @return true if the time component is between {@link #BUSINESS_HOURS_START}
     *         and {@link #BUSINESS_HOURS_END}
     */
    public static boolean isWithinBusinessHours(LocalDateTime dateTime) {
        LocalTime time = dateTime.toLocalTime();
        return !time.isBefore(BUSINESS_HOURS_START) && time.isBefore(BUSINESS_HOURS_END);
    }

    /**
     * Check if the current day is a weekday (Monday through Friday).
     *
     * @return true if today is a weekday
     */
    public static boolean isWeekday() {
        DayOfWeek today = LocalDateTime.now().getDayOfWeek();
        boolean weekday = today.getValue() >= WORK_WEEK_START.getValue()
                && today.getValue() <= WORK_WEEK_END.getValue();
        log.debug("Weekday check: today={}, isWeekday={}", today, weekday);
        return weekday;
    }

    /**
     * Check if the given timestamp falls on a weekday.
     *
     * @param dateTime the timestamp to check
     * @return true if the day is Monday through Friday
     */
    public static boolean isWeekday(LocalDateTime dateTime) {
        DayOfWeek day = dateTime.getDayOfWeek();
        return day.getValue() >= WORK_WEEK_START.getValue()
                && day.getValue() <= WORK_WEEK_END.getValue();
    }

    /**
     * Check if the current time is within standard working hours
     * (weekday AND within business hours).
     *
     * @return true if currently within working hours on a weekday
     */
    public static boolean isWithinWorkingHours() {
        return isWeekday() && isWithinBusinessHours();
    }

    /**
     * Check if the given timestamp falls within standard working hours
     * (weekday AND within business hours).
     *
     * @param dateTime the timestamp to check
     * @return true if the timestamp is within working hours on a weekday
     */
    public static boolean isWithinWorkingHours(LocalDateTime dateTime) {
        return isWeekday(dateTime) && isWithinBusinessHours(dateTime);
    }
}
