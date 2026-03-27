package com.propertize.platform.employecraft.repository;

import com.propertize.platform.employecraft.entity.Employee;
import com.propertize.platform.employecraft.enums.EmployeeStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    @EntityGraph(attributePaths = { "department", "manager" })
    Page<Employee> findByOrganizationId(UUID organizationId, Pageable pageable);

    List<Employee> findByOrganizationIdAndStatus(UUID organizationId, EmployeeStatusEnum status);

    @EntityGraph(attributePaths = { "department", "manager" })
    Optional<Employee> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<Employee> findByEmployeeNumberAndOrganizationId(String employeeNumber, UUID organizationId);

    Optional<Employee> findByEmailAndOrganizationId(String email, UUID organizationId);

    Optional<Employee> findByUserId(Long userId);

    Optional<Employee> findByUserIdAndOrganizationId(Long userId, UUID organizationId);

    boolean existsByEmployeeNumberAndOrganizationId(String employeeNumber, UUID organizationId);

    boolean existsByEmailAndOrganizationId(String email, UUID organizationId);

    @Query("SELECT COUNT(e) FROM Employee e WHERE e.organizationId = :orgId AND e.status = :status")
    long countByOrganizationIdAndStatus(@Param("orgId") UUID organizationId,
            @Param("status") EmployeeStatusEnum status);

    @Query("SELECT e FROM Employee e WHERE e.organizationId = :orgId AND e.department.id = :deptId")
    List<Employee> findByOrganizationIdAndDepartmentId(@Param("orgId") UUID organizationId,
            @Param("deptId") UUID departmentId);

    @Query("SELECT e FROM Employee e WHERE e.organizationId = :orgId AND e.manager.id = :managerId")
    List<Employee> findByOrganizationIdAndManagerId(@Param("orgId") UUID organizationId,
            @Param("managerId") UUID managerId);
}
