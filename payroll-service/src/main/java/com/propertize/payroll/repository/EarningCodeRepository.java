package com.propertize.payroll.repository;

import com.propertize.payroll.entity.EarningCodeEntity;
import com.propertize.payroll.enums.EarningTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EarningCodeRepository extends JpaRepository<EarningCodeEntity, UUID> {

    List<EarningCodeEntity> findByClientId(UUID clientId);

    Optional<EarningCodeEntity> findByClientIdAndEarningCode(UUID clientId, String earningCode);

    @Query("SELECT ec FROM EarningCodeEntity ec WHERE ec.client.id = :clientId AND ec.isActive = true")
    List<EarningCodeEntity> findActiveByClientId(@Param("clientId") UUID clientId);

    @Query("SELECT ec FROM EarningCodeEntity ec WHERE ec.client.id = :clientId AND ec.earningType = :type")
    List<EarningCodeEntity> findByClientIdAndEarningType(
            @Param("clientId") UUID clientId,
            @Param("type") EarningTypeEnum type);
}
