package com.propertize.payroll.repository;

import com.propertize.payroll.entity.DepartmentEntity;
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
 * Repository for Department entity operations.
 */
@Repository
public interface DepartmentRepository extends JpaRepository<DepartmentEntity, UUID> {

    /**
     * Find all departments for a specific client with pagination.
     */
    Page<DepartmentEntity> findByClientId(UUID clientId, Pageable pageable);

    /**
     * Find all departments for a specific client.
     */
    List<DepartmentEntity> findByClientId(UUID clientId);

    /**
     * Find department by client ID and department code.
     */
    Optional<DepartmentEntity> findByClientIdAndDepartmentCode(UUID clientId, String departmentCode);

    /**
     * Find active departments for a client.
     */
    List<DepartmentEntity> findByClientIdAndIsActiveTrue(UUID clientId);

    /**
     * Find departments by parent department.
     */
    List<DepartmentEntity> findByParentDepartmentId(UUID parentDepartmentId);

    /**
     * Check if department code exists for a client.
     */
    boolean existsByClientIdAndDepartmentCode(UUID clientId, String departmentCode);

    /**
     * Find departments with their employee counts.
     */
    @Query("SELECT d FROM DepartmentEntity d LEFT JOIN FETCH d.client WHERE d.client.id = :clientId AND d.isActive = true")
    List<DepartmentEntity> findActiveDepartmentsWithClient(@Param("clientId") UUID clientId);

    /**
     * Find top-level departments (no parent) for a client.
     */
    List<DepartmentEntity> findByClientIdAndParentDepartmentIsNullAndIsActiveTrue(UUID clientId);
}
