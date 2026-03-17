package com.propertize.platform.auth.repository;

import com.propertize.platform.auth.entity.AuditResult;
import com.propertize.platform.auth.entity.PermissionAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for {@link PermissionAuditLog} entities.
 *
 * <p>
 * Provides query methods for compliance auditing, access-denial
 * investigations, and usage-summary reports.
 * </p>
 *
 * @version 1.0 - Phase 4b: Permission Audit Trail
 */
@Repository
public interface PermissionAuditLogRepository extends JpaRepository<PermissionAuditLog, Long> {

    /**
     * Find audit logs for a specific user within a date range.
     */
    List<PermissionAuditLog> findByUserIdAndCreatedAtBetween(
            Long userId, LocalDateTime from, LocalDateTime to);

    /**
     * Find audit logs for a specific permission within a date range.
     */
    List<PermissionAuditLog> findByPermissionAndCreatedAtBetween(
            String permission, LocalDateTime from, LocalDateTime to);

    /**
     * Find audit logs by result (e.g., DENIED) within a date range.
     */
    List<PermissionAuditLog> findByResultAndCreatedAtBetween(
            AuditResult result, LocalDateTime from, LocalDateTime to);

    /**
     * Find audit logs for an organization within a date range.
     */
    List<PermissionAuditLog> findByOrganizationIdAndCreatedAtBetween(
            Long orgId, LocalDateTime from, LocalDateTime to);

    /**
     * Find audit logs for a user with a specific result after a given timestamp.
     * Useful for queries like "show me all denials for user X in the last 7 days".
     */
    List<PermissionAuditLog> findByUserIdAndResultAndCreatedAtAfter(
            Long userId, AuditResult result, LocalDateTime after);

    /**
     * Count audit logs by result within a date range.
     */
    long countByResultAndCreatedAtBetween(
            AuditResult result, LocalDateTime from, LocalDateTime to);

    /**
     * Find the most frequently denied permissions since a given date.
     *
     * @param from  the start of the time window
     * @param limit maximum number of results to return
     * @return list of permission strings ordered by denial frequency (descending)
     */
    @Query(value = "SELECT p.permission FROM permission_audit_logs p " +
            "WHERE p.result = 'DENIED' AND p.created_at >= :from " +
            "GROUP BY p.permission ORDER BY COUNT(*) DESC LIMIT :limit", nativeQuery = true)
    List<String> findTopDeniedPermissions(
            @Param("from") LocalDateTime from,
            @Param("limit") int limit);

    /**
     * Find the most frequently accessed resources since a given date.
     *
     * @param from  the start of the time window
     * @param limit maximum number of results to return
     * @return list of resource type + resource ID pairs ordered by access frequency
     *         (descending)
     */
    @Query(value = "SELECT CONCAT(p.resource_type, ':', p.resource_id) FROM permission_audit_logs p " +
            "WHERE p.resource_type IS NOT NULL AND p.resource_id IS NOT NULL " +
            "AND p.created_at >= :from " +
            "GROUP BY p.resource_type, p.resource_id ORDER BY COUNT(*) DESC LIMIT :limit", nativeQuery = true)
    List<String> findTopAccessedResources(
            @Param("from") LocalDateTime from,
            @Param("limit") int limit);
}
