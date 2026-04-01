package com.propertize.platform.employecraft.repository;

import com.propertize.platform.employecraft.entity.Position;
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
public interface PositionRepository extends JpaRepository<Position, UUID> {

    @Query("SELECT p FROM Position p WHERE p.organizationId = CAST(:orgId AS uuid)")
    Page<Position> findByOrganizationId(@Param("orgId") String orgId, Pageable pageable);

    @Query("SELECT p FROM Position p WHERE p.organizationId = CAST(:orgId AS uuid) AND p.isActive = true")
    List<Position> findByOrganizationIdAndIsActiveTrue(@Param("orgId") String orgId);

    @Query("SELECT p FROM Position p WHERE p.id = CAST(:id AS uuid) AND p.organizationId = CAST(:orgId AS uuid)")
    Optional<Position> findByIdAndOrganizationId(@Param("id") String id, @Param("orgId") String orgId);

    @Query("SELECT p FROM Position p WHERE p.code = :code AND p.organizationId = CAST(:orgId AS uuid)")
    Optional<Position> findByCodeAndOrganizationId(@Param("code") String code, @Param("orgId") String orgId);

    @Query("SELECT COUNT(p) FROM Position p WHERE p.code = :code AND p.organizationId = CAST(:orgId AS uuid)")
    long countByCodeAndOrganizationId(@Param("code") String code, @Param("orgId") String orgId);
}
