package com.propertize.platform.employecraft.repository;

import com.propertize.platform.employecraft.entity.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, UUID> {

    @Query("SELECT d FROM Department d WHERE d.organizationId = CAST(:orgId AS uuid)")
    Page<Department> findByOrganizationId(@Param("orgId") String orgId, Pageable pageable);

    @Query("SELECT d FROM Department d WHERE d.organizationId = CAST(:orgId AS uuid) AND d.isActive = true")
    List<Department> findByOrganizationIdAndIsActiveTrue(@Param("orgId") String orgId);

    @Query("SELECT d FROM Department d WHERE d.id = CAST(:id AS uuid) AND d.organizationId = CAST(:orgId AS uuid)")
    Optional<Department> findByIdAndOrganizationId(@Param("id") String id, @Param("orgId") String orgId);

    @Query("SELECT d FROM Department d WHERE d.code = :code AND d.organizationId = CAST(:orgId AS uuid)")
    Optional<Department> findByCodeAndOrganizationId(@Param("code") String code, @Param("orgId") String orgId);

    @Query("SELECT COUNT(d) FROM Department d WHERE d.code = :code AND d.organizationId = CAST(:orgId AS uuid)")
    long countByCodeAndOrganizationId(@Param("code") String code, @Param("orgId") String orgId);
}
