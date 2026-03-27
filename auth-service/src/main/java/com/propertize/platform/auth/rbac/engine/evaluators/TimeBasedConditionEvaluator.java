package com.propertize.platform.auth.rbac.engine.evaluators;

import com.propertize.platform.auth.config.RbacConfig;
import com.propertize.platform.auth.rbac.engine.ConditionEvaluator;
import com.propertize.platform.auth.rbac.engine.PolicyContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.*;
import java.util.Map;
import java.util.Set;

/**
 * Evaluates time-based access conditions.
 * Restricts access by time-of-day, day-of-week per role.
 *
 * Reads time_restrictions from rbac.yml:
 * time_restrictions:
 * MAINTENANCE_COORDINATOR:
 * active_hours: "08:00-18:00"
 * active_days: ["MON","TUE","WED","THU","FRI"]
 * timezone: "America/New_York"
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TimeBasedConditionEvaluator implements ConditionEvaluator {

    private final RbacConfig rbacConfig;

    @Override
    public boolean evaluate(PolicyContext context, String condition, Map<String, Object> attributes) {
        if (!"time_restriction".equals(condition)) {
            return true;
        }

        Map<String, RbacConfig.TimeRestriction> restrictions = rbacConfig.getTimeRestrictions();
        if (restrictions == null || restrictions.isEmpty()) {
            return true; // No time restrictions configured
        }

        Set<String> roles = context.getRoles();
        if (roles == null || roles.isEmpty()) {
            return true;
        }

        for (String role : roles) {
            RbacConfig.TimeRestriction restriction = restrictions.get(role);
            if (restriction != null && !isWithinAllowedTime(restriction)) {
                log.debug("Time restriction denied for role {} at current time", role);
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean supports(String condition) {
        return "time_restriction".equals(condition);
    }

    /**
     * Check if any of the user's roles have time restrictions that block access
     * now.
     */
    public boolean isAccessAllowed(Set<String> roles) {
        Map<String, RbacConfig.TimeRestriction> restrictions = rbacConfig.getTimeRestrictions();
        if (restrictions == null || restrictions.isEmpty()) {
            return true;
        }

        for (String role : roles) {
            RbacConfig.TimeRestriction restriction = restrictions.get(role);
            if (restriction != null && !isWithinAllowedTime(restriction)) {
                return false;
            }
        }
        return true;
    }

    private boolean isWithinAllowedTime(RbacConfig.TimeRestriction restriction) {
        ZoneId zone = ZoneId.of(
                restriction.getTimezone() != null ? restriction.getTimezone() : "UTC");
        ZonedDateTime now = ZonedDateTime.now(zone);

        // Check day-of-week
        if (restriction.getActiveDays() != null && !restriction.getActiveDays().isEmpty()) {
            String currentDay = now.getDayOfWeek().name().substring(0, 3); // MON, TUE, etc.
            if (!restriction.getActiveDays().contains(currentDay)) {
                log.debug("Access denied: current day {} not in allowed days {}", currentDay,
                        restriction.getActiveDays());
                return false;
            }
        }

        // Check time-of-day
        if (restriction.getActiveHours() != null && !restriction.getActiveHours().isBlank()) {
            String[] parts = restriction.getActiveHours().split("-");
            if (parts.length == 2) {
                LocalTime start = LocalTime.parse(parts[0].trim());
                LocalTime end = LocalTime.parse(parts[1].trim());
                LocalTime currentTime = now.toLocalTime();

                if (start.isBefore(end)) {
                    // Normal range (e.g., 08:00-18:00)
                    if (currentTime.isBefore(start) || currentTime.isAfter(end)) {
                        log.debug("Access denied: current time {} outside allowed hours {}", currentTime,
                                restriction.getActiveHours());
                        return false;
                    }
                } else {
                    // Overnight range (e.g., 22:00-06:00)
                    if (currentTime.isBefore(start) && currentTime.isAfter(end)) {
                        log.debug("Access denied: current time {} outside allowed hours {}", currentTime,
                                restriction.getActiveHours());
                        return false;
                    }
                }
            }
        }

        return true;
    }
}
