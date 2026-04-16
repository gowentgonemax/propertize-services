package com.propertize.payroll.repository;

import com.propertize.payroll.entity.EmployeeEntity;
import com.propertize.commons.enums.employee.EmployeeStatusEnum;
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
public interface EmployeeEntityRepository extends JpaRepository<EmployeeEntity, UUID> {

        Optional<EmployeeEntity> findByEmployeeNumber(String employeeNumber);

        Optional<EmployeeEntity> findByExternalEmployeeId(UUID externalEmployeeId);

        @EntityGraph(attributePaths = { "client" })
        Page<EmployeeEntity> findByClientId(UUID clientId, Pageable pageable);

        @EntityGraph(attributePaths = { "client" })
        List<EmployeeEntity> findByClientIdAndStatus(UUID clientId, EmployeeStatusEnum status);

        @Query("SELECT e FROM EmployeeEntity e WHERE e.client.id = :clientId AND e.departmentId = :departmentId")
        List<EmployeeEntity> findByClientIdAndDepartmentId(@Param("clientId") UUID clientId,
                        @Param("departmentId") UUID departmentId);

        @Query("SELECT e FROM EmployeeEntity e WHERE e.client.id = :clientId AND " +
                        "(LOWER(e.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "LOWER(e.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
                        "LOWER(e.employeeNumber) LIKE LOWER(CONCAT('%', :search, '%')))")
        Page<EmployeeEntity> searchByClientId(@Param("clientId") UUID clientId, @Param("search") String search,
                        Pageable pageable);

        @Query("SELECT COUNT(e) FROM EmployeeEntity e WHERE e.client.id = :clientId AND e.status = :status")
        long countByClientIdAndStatus(@Param("clientId") UUID clientId, @Param("status") EmployeeStatusEnum status);

        boolean existsByEmployeeNumber(String employeeNumber);

        boolean existsBySsnLastFour(String ssnLastFour);
}
