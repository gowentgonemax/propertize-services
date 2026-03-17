package com.propertize.platform.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Summary DTO for permission audit reporting.
 *
 * <p>
 * Provides a high-level overview of permission activity within a
 * time period, suitable for compliance dashboards and executive reports.
 * </p>
 *
 * @version 1.0 - Phase 4b: Permission Audit Trail
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditSummaryDTO {

    /**
     * Total number of permission checks in the period.
     */
    private long totalChecks;

    /**
     * Total number of allowed (successful) permission checks.
     */
    private long totalAllowed;

    /**
     * Total number of denied permission checks.
     */
    private long totalDenied;

    /**
     * Permissions that were most frequently denied, ordered by frequency.
     */
    private List<String> topDeniedPermissions;

    /**
     * Resources that were most frequently accessed, ordered by frequency.
     */
    private List<String> topAccessedResources;

    /**
     * Start of the reporting period.
     */
    private LocalDateTime periodStart;

    /**
     * End of the reporting period.
     */
    private LocalDateTime periodEnd;
}
