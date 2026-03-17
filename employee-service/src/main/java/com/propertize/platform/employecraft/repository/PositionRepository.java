package com.propertize.platform.employecraft.repository;

import com.propertize.platform.employecraft.entity.Position;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PositionRepository extends JpaRepository<Position, UUID> {

    Page<Position> findByOrganizationId(UUID organizationId, Pageable pageable);

    List<Position> findByOrganizationIdAndIsActiveTrue(UUID organizationId);

    Optional<Position> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<Position> findByCodeAndOrganizationId(String code, UUID organizationId);

    boolean existsByCodeAndOrganizationId(String code, UUID organizationId);
}
