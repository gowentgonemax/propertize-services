package com.propertize.platform.employecraft.repository;

import com.propertize.platform.employecraft.entity.Employee;
import com.propertize.commons.enums.employee.EmployeeStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

        @EntityGraph(attributePaths = { "department", "position", "manager" })
        @Query("SELECT e FROM Employee e WHERE e.organizationId = CAST(:orgId AS uuid)")
        Page<Employee> findByOrganizationId(@Param("orgId") String orgId, Pageable pageable);

        @EntityGraph(attributePaths = { "department", "position", "manager" })
        @Query("SELECT e FROM Employee e WHERE e.organizationId = CAST(:orgId AS uuid) AND e.status = :status")
        List<Employee> findByOrganizationIdAndStatus(@Param("orgId") String orgId,
                        @Param("status") EmployeeStatusEnum status);

        @Query("SELECT e FROM Employee e WHERE e.id = CAST(:id AS uuid) AND e.organizationId = CAST(:orgId AS uuid)")
        Optional<Employee> findByIdAndOrganizationId(@Param("id") String id, @Param("orgId") String orgId);

        @Query("SELECT e FROM Employee e WHERE e.employeeNumber = :empNum AND e.organizationId = CAST(:orgId AS uuid)")
        Optional<Employee> findByEmployeeNumberAndOrganizationId(@Param("empNum") String employeeNumber,
                        @Param("orgId") String orgId);

        @Query("SELECT e FROM Employee e WHERE e.email = :email AND e.organizationId = CAST(:orgId AS uuid)")
        Optional<Employee> findByEmailAndOrganizationId(@Param("email") String email, @Param("orgId") String orgId);

        Optional<Employee> findByUserId(Long userId);

        @Query("SELECT e FROM Employee e WHERE e.userId = :userId AND e.organizationId = CAST(:orgId AS uuid)")
        Optional<Employee> findByUserIdAndOrganizationId(@Param("userId") Long userId, @Param("orgId") String orgId);

        @Query("SELECT COUNT(e) FROM Employee e WHERE e.employeeNumber = :empNum AND e.organizationId = CAST(:orgId AS uuid)")
        long countByEmployeeNumberAndOrganizationId(@Param("empNum") String employeeNumber,
                        @Param("orgId") String orgId);

        @Query("SELECT COUNT(e) FROM Employee e WHERE e.email = :email AND e.organizationId = CAST(:orgId AS uuid)")
        long countByEmailAndOrganizationId(@Param("email") String email, @Param("orgId") String orgId);

        @Query("SELECT COUNT(e) FROM Employee e WHERE e.organizationId = CAST(:orgId AS uuid) AND e.status = :status")
        long countByOrganizationIdAndStatus(@Param("orgId") String orgId,
                        @Param("status") EmployeeStatusEnum status);

        @EntityGraph(attributePaths = { "department", "position", "manager" })
        @Query("SELECT e FROM Employee e WHERE e.organizationId = CAST(:orgId AS uuid) AND e.department.id = CAST(:deptId AS uuid)")
        List<Employee> findByOrganizationIdAndDepartmentId(@Param("orgId") String orgId,
                        @Param("deptId") String departmentId);

        @EntityGraph(attributePaths = { "department", "position", "manager" })
        @Query("SELECT e FROM Employee e WHERE e.organizationId = CAST(:orgId AS uuid) AND e.manager.id = CAST(:managerId AS uuid)")
        List<Employee> findByOrganizationIdAndManagerId(@Param("orgId") String orgId,
                        @Param("managerId") String managerId);

        @Query("SELECT e FROM Employee e WHERE e.organizationId = CAST(:orgId AS uuid) AND e.updatedAt > :since")
        Page<Employee> findByOrganizationIdAndUpdatedAtAfter(@Param("orgId") String orgId,
                        @Param("since") LocalDateTime since, Pageable pageable);

        @EntityGraph(attributePaths = { "department", "position", "manager" })
        @Query("SELECT e FROM Employee e WHERE e.organizationId = CAST(:orgId AS uuid) AND e.status IN :statuses")
        List<Employee> findByOrganizationIdAndStatusIn(@Param("orgId") String orgId,
                        @Param("statuses") List<EmployeeStatusEnum> statuses);
}
