package com.propertize.payroll.repository;

import com.propertize.payroll.entity.PositionEntity;
import com.propertize.payroll.enums.PositionStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Position entity operations.
 */
@Repository
public interface PositionRepository extends JpaRepository<PositionEntity, UUID> {

    /**
     * Find all positions for a specific client with pagination.
     */
    Page<PositionEntity> findByClientId(UUID clientId, Pageable pageable);

    /**
     * Find all positions for a specific client.
     */
    List<PositionEntity> findByClientId(UUID clientId);

    /**
     * Find position by client ID and position code.
     */
    Optional<PositionEntity> findByClientIdAndPositionCode(UUID clientId, String positionCode);

    /**
     * Find active positions for a client.
     */
    List<PositionEntity> findByClientIdAndStatus(UUID clientId, PositionStatusEnum status);

    /**
     * Find positions by department.
     */
    List<PositionEntity> findByDepartmentId(UUID departmentId);

    /**
     * Find positions by job family.
     */
    List<PositionEntity> findByClientIdAndJobFamily(UUID clientId, String jobFamily);

    /**
     * Find positions by job level.
     */
    List<PositionEntity> findByClientIdAndJobLevel(UUID clientId, Integer jobLevel);

    /**
     * Check if position code exists for a client.
     */
    boolean existsByClientIdAndPositionCode(UUID clientId, String positionCode);

    /**
     * Find positions with open headcount.
     */
    @Query("SELECT p FROM PositionEntity p WHERE p.client.id = :clientId " +
           "AND p.status = 'ACTIVE' " +
           "AND (p.headcountBudget IS NULL OR p.headcountActual IS NULL OR p.headcountActual < p.headcountBudget)")
    List<PositionEntity> findPositionsWithOpenHeadcount(@Param("clientId") UUID clientId);

    /**
     * Find positions that report to a specific position.
     */
    List<PositionEntity> findByReportsToPositionId(UUID reportsToPositionId);

    /**
     * Count employees in a position.
     */
    @Query("SELECT COUNT(e) FROM EmployeeEntity e WHERE e.position.id = :positionId")
    Long countEmployeesByPositionId(@Param("positionId") UUID positionId);
}
