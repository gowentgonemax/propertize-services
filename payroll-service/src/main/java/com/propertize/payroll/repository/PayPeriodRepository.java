package com.propertize.payroll.repository;

import com.propertize.payroll.entity.PayPeriodEntity;
import com.propertize.payroll.enums.PayFrequencyEnum;
import com.propertize.payroll.enums.PayPeriodStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for PayPeriod entity operations.
 */
@Repository
public interface PayPeriodRepository extends JpaRepository<PayPeriodEntity, UUID> {

    /**
     * Find all pay periods for a specific client with pagination.
     */
    Page<PayPeriodEntity> findByClientId(UUID clientId, Pageable pageable);

    /**
     * Find all pay periods for a specific client.
     */
    List<PayPeriodEntity> findByClientId(UUID clientId);

    /**
     * Find pay period by client ID and date range.
     */
    @Query("SELECT pp FROM PayPeriodEntity pp WHERE pp.client.id = :clientId " +
           "AND pp.period.startDate = :startDate AND pp.period.endDate = :endDate")
    Optional<PayPeriodEntity> findByClientIdAndPeriodDates(
        @Param("clientId") UUID clientId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Find pay periods by status.
     */
    List<PayPeriodEntity> findByStatus(PayPeriodStatusEnum status);

    /**
     * Find pay periods by client and status.
     */
    List<PayPeriodEntity> findByClientIdAndStatus(UUID clientId, PayPeriodStatusEnum status);

    /**
     * Find pay period containing a specific date.
     */
    @Query("SELECT pp FROM PayPeriodEntity pp WHERE pp.client.id = :clientId " +
           "AND :date BETWEEN pp.period.startDate AND pp.period.endDate")
    Optional<PayPeriodEntity> findByClientIdAndDateWithin(
        @Param("clientId") UUID clientId,
        @Param("date") LocalDate date
    );

    /**
     * Find current/active pay period.
     */
    @Query("SELECT pp FROM PayPeriodEntity pp WHERE pp.client.id = :clientId " +
           "AND CURRENT_DATE BETWEEN pp.period.startDate AND pp.period.endDate")
    Optional<PayPeriodEntity> findCurrentPayPeriod(@Param("clientId") UUID clientId);

    /**
     * Find upcoming pay periods (start date in future).
     */
    @Query("SELECT pp FROM PayPeriodEntity pp WHERE pp.client.id = :clientId " +
           "AND pp.period.startDate > CURRENT_DATE ORDER BY pp.period.startDate ASC")
    List<PayPeriodEntity> findUpcomingPayPeriods(@Param("clientId") UUID clientId);

    /**
     * Find pay periods by fiscal year.
     */
    List<PayPeriodEntity> findByClientIdAndFiscalYear(UUID clientId, Integer fiscalYear);

    /**
     * Find pay periods by pay frequency.
     */
    List<PayPeriodEntity> findByClientIdAndPayFrequency(UUID clientId, PayFrequencyEnum payFrequency);

    /**
     * Find pay periods with upcoming pay dates.
     */
    @Query("SELECT pp FROM PayPeriodEntity pp WHERE pp.client.id = :clientId " +
           "AND pp.payDate BETWEEN :startDate AND :endDate ORDER BY pp.payDate ASC")
    List<PayPeriodEntity> findByClientIdAndPayDateBetween(
        @Param("clientId") UUID clientId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * Count open pay periods for a client.
     */
    Long countByClientIdAndStatus(UUID clientId, PayPeriodStatusEnum status);
}
