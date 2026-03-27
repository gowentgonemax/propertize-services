package com.propertize.payroll.repository;

import com.propertize.payroll.entity.TaxCalculationEntity;
import com.propertize.payroll.enums.TaxTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface TaxCalculationRepository extends JpaRepository<TaxCalculationEntity, UUID> {

    List<TaxCalculationEntity> findByPaystubId(UUID paystubId);

    @Query("SELECT tc FROM TaxCalculationEntity tc WHERE tc.paystub.id = :paystubId AND tc.taxType = :taxType")
    List<TaxCalculationEntity> findByPaystubIdAndTaxType(
            @Param("paystubId") UUID paystubId,
            @Param("taxType") TaxTypeEnum taxType);

    @Query("SELECT SUM(tc.finalTaxAmount) FROM TaxCalculationEntity tc WHERE tc.paystub.id = :paystubId")
    BigDecimal sumTaxesByPaystubId(@Param("paystubId") UUID paystubId);

    @Query("SELECT tc FROM TaxCalculationEntity tc WHERE tc.paystub.payrollRun.id = :payrollRunId")
    List<TaxCalculationEntity> findByPayrollRunId(@Param("payrollRunId") UUID payrollRunId);
}
