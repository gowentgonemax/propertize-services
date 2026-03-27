package com.propertize.payroll.repository;

import com.propertize.payroll.entity.PayDifferentialEntity;
import com.propertize.payroll.enums.DifferentialStatusEnum;
import com.propertize.payroll.enums.DifferentialTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ShiftDifferentialRepository extends JpaRepository<PayDifferentialEntity, UUID> {

    List<PayDifferentialEntity> findByClientId(UUID clientId);

    List<PayDifferentialEntity> findByClientIdAndStatus(UUID clientId, DifferentialStatusEnum status);

    @Query("SELECT pd FROM PayDifferentialEntity pd WHERE pd.client.id = :clientId AND pd.differentialType = :type AND pd.status = 'ACTIVE'")
    List<PayDifferentialEntity> findActiveByClientIdAndType(
        @Param("clientId") UUID clientId,
        @Param("type") DifferentialTypeEnum type);

    @Query("SELECT pd FROM PayDifferentialEntity pd WHERE pd.client.id = :clientId AND pd.status = 'ACTIVE' AND " +
           "pd.startTime <= :time AND pd.endTime >= :time")
    List<PayDifferentialEntity> findApplicableDifferentials(
        @Param("clientId") UUID clientId,
        @Param("time") LocalTime time);
}
