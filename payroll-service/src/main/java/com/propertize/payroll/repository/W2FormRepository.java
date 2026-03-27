package com.propertize.payroll.repository;

import com.propertize.payroll.entity.W2FormEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface W2FormRepository extends JpaRepository<W2FormEntity, UUID> {

    List<W2FormEntity> findByEmployeeId(UUID employeeId);

    @Query("SELECT w FROM W2FormEntity w WHERE w.employee.id = :employeeId AND w.taxYear = :year")
    Optional<W2FormEntity> findByEmployeeIdAndTaxYear(
            @Param("employeeId") UUID employeeId,
            @Param("year") Integer year);

    @Query("SELECT w FROM W2FormEntity w WHERE w.client.id = :clientId AND w.taxYear = :year ORDER BY w.employee.id")
    List<W2FormEntity> findByClientIdAndTaxYear(
            @Param("clientId") UUID clientId,
            @Param("year") Integer year);

    @Query("SELECT w FROM W2FormEntity w WHERE w.taxYear = :year AND w.formStatus = 'FINALIZED'")
    List<W2FormEntity> findFinalizedByTaxYear(@Param("year") Integer year);

    @Query("SELECT COUNT(w) FROM W2FormEntity w WHERE w.client.id = :clientId AND w.taxYear = :year AND w.formStatus <> 'FINALIZED'")
    long countPendingW2sByClientIdAndYear(
            @Param("clientId") UUID clientId,
            @Param("year") Integer year);
}
