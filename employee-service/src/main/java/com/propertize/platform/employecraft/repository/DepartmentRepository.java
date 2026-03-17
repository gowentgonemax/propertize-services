package com.propertize.platform.employecraft.repository;

import com.propertize.platform.employecraft.entity.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, UUID> {

    Page<Department> findByOrganizationId(UUID organizationId, Pageable pageable);

    List<Department> findByOrganizationIdAndIsActiveTrue(UUID organizationId);

    Optional<Department> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<Department> findByCodeAndOrganizationId(String code, UUID organizationId);

    boolean existsByCodeAndOrganizationId(String code, UUID organizationId);
}
