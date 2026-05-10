package com.propertize.payment.repository;

import com.propertize.payment.entity.Payment;
import com.propertize.commons.enums.payment.PaymentCategoryEnum;
import com.propertize.commons.enums.payment.PaymentContextEnum;
import com.propertize.commons.enums.payment.PaymentStatusEnum;
import com.propertize.payment.repository.projection.PaymentStatisticsProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {

  Page<Payment> findByOrganizationId(String organizationId, Pageable pageable);

  Page<Payment> findByOrganizationIdAndTenantId(String organizationId, String tenantId, Pageable pageable);

  List<Payment> findByTenantId(String tenantId);

  List<Payment> findByLeaseId(String leaseId);

  List<Payment> findByStatus(PaymentStatusEnum status);

  List<Payment> findByVendorId(String vendorId);

  List<Payment> findByMaintenanceRequestId(String maintenanceRequestId);

  @Query("SELECT p FROM Payment p WHERE p.paymentCategory = 'PLATFORM_SUBSCRIPTION'")
  List<Payment> findAllPlatformSubscriptionPayments();

  @Query("SELECT p FROM Payment p WHERE p.paymentCategory = 'PLATFORM_SUBSCRIPTION' AND p.organizationId = :orgId")
  List<Payment> findPlatformSubscriptionPaymentsByOrganization(@Param("orgId") String orgId);

  @Query("SELECT p FROM Payment p WHERE p.tenantId = :tenantId AND p.paymentDate BETWEEN :start AND :end")
  List<Payment> findByTenantIdAndDateRange(
      @Param("tenantId") String tenantId,
      @Param("start") LocalDate start,
      @Param("end") LocalDate end);

  @Query("SELECT p FROM Payment p WHERE p.paymentContext = :context AND p.paymentCategory = :category AND p.organizationId = :orgId")
  Page<Payment> findByContextAndCategory(
      @Param("context") PaymentContextEnum context,
      @Param("category") PaymentCategoryEnum category,
      @Param("orgId") String orgId,
      Pageable pageable);

  List<Payment> findByPaymentCategory(PaymentCategoryEnum category);

  List<Payment> findByPaymentContext(PaymentContextEnum context);

  @Query("SELECT p FROM Payment p WHERE p.status = 'FAILED' AND p.organizationId IS NOT NULL")
  Page<Payment> findFailedPaymentsForRetry(Pageable pageable);

  @Query("SELECT p FROM Payment p WHERE p.ownerId = :ownerId AND p.paymentCategory = 'OWNER_PAYOUT'")
  List<Payment> findOwnerPayouts(@Param("ownerId") Long ownerId);

  @Query("SELECT p FROM Payment p WHERE p.ownerId = :ownerId AND p.propertyId = :propertyId AND p.paymentCategory = 'OWNER_PAYOUT'")
  List<Payment> findOwnerPayoutsByProperty(@Param("ownerId") Long ownerId,
      @Param("propertyId") String propertyId);

  @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.tenantId = :tenantId AND p.status = 'COMPLETED'")
  Double sumCompletedAmountByTenant(@Param("tenantId") String tenantId);

  Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);

  Optional<Payment> findByStripeChargeId(String stripeChargeId);

  // ──────────────────── Statistics ────────────────────────────────────

  /**
   * Aggregates payment counts and sums for a given organisation,
   * with optional date-range and entity-scope filters.
   *
   * <p>
   * NULL parameters are treated as "no filter" via native SQL
   * {@code IS NULL OR} guards, which PostgreSQL evaluates at plan time.
   */
  @Query(value = """
      SELECT
          COUNT(*)                                                       AS totalPayments,
          SUM(CASE WHEN status = 'COMPLETED' THEN 1 ELSE 0 END)          AS completedPayments,
          SUM(CASE WHEN status = 'FAILED' THEN 1 ELSE 0 END)             AS failedPayments,
          SUM(CASE WHEN status IN ('PENDING','PROCESSING') THEN 1 ELSE 0 END) AS pendingPayments,
          SUM(CASE WHEN status IN ('REFUNDED','PARTIALLY_REFUNDED') THEN 1 ELSE 0 END) AS refundedPayments,
          COALESCE(SUM(amount), 0)                                       AS totalAmount,
          COALESCE(SUM(CASE WHEN status = 'COMPLETED' THEN amount ELSE 0 END), 0)    AS completedAmount,
          COALESCE(SUM(CASE WHEN status IN ('PENDING','PROCESSING') THEN amount ELSE 0 END), 0) AS pendingAmount,
          COALESCE(SUM(CASE WHEN status = 'FAILED' THEN amount ELSE 0 END), 0)       AS failedAmount,
          COALESCE(SUM(CASE WHEN status IN ('REFUNDED','PARTIALLY_REFUNDED') THEN amount ELSE 0 END), 0) AS refundedAmount,
          COALESCE(AVG(amount), 0)                                       AS averageAmount
      FROM payment
      WHERE organization_id = :orgId
        AND payment_date >= COALESCE(CAST(:start AS DATE), DATE '0001-01-01')
        AND payment_date <= COALESCE(CAST(:end   AS DATE), DATE '9999-12-31')
        AND (CAST(:tenantId AS TEXT) IS NULL OR tenant_id = CAST(:tenantId AS TEXT))
        AND (CAST(:leaseId  AS TEXT) IS NULL OR lease_id  = CAST(:leaseId  AS TEXT))
      """, nativeQuery = true)
  PaymentStatisticsProjection computeStatistics(
      @Param("orgId") String orgId,
      @Param("start") String start,
      @Param("end") String end,
      @Param("tenantId") String tenantId,
      @Param("leaseId") String leaseId);

  /**
   * Count-by-payment-method breakdown for the statistics endpoint.
   * Returns rows of (payment_method, count).
   */
  @Query(value = """
      SELECT payment_method AS method, COUNT(*) AS cnt
      FROM payment
      WHERE organization_id = :orgId
        AND payment_date >= COALESCE(CAST(:start AS DATE), DATE '0001-01-01')
        AND payment_date <= COALESCE(CAST(:end   AS DATE), DATE '9999-12-31')
        AND (CAST(:tenantId AS TEXT) IS NULL OR tenant_id = CAST(:tenantId AS TEXT))
        AND (CAST(:leaseId  AS TEXT) IS NULL OR lease_id  = CAST(:leaseId  AS TEXT))
      GROUP BY payment_method
      """, nativeQuery = true)
  List<Object[]> countByMethod(
      @Param("orgId") String orgId,
      @Param("start") String start,
      @Param("end") String end,
      @Param("tenantId") String tenantId,
      @Param("leaseId") String leaseId);

  /**
   * Count-by-payment-category breakdown for the statistics endpoint.
   */
  @Query(value = """
      SELECT payment_category AS category, COUNT(*) AS cnt
      FROM payment
      WHERE organization_id = :orgId
        AND payment_date >= COALESCE(CAST(:start AS DATE), DATE '0001-01-01')
        AND payment_date <= COALESCE(CAST(:end   AS DATE), DATE '9999-12-31')
        AND (CAST(:tenantId AS TEXT) IS NULL OR tenant_id = CAST(:tenantId AS TEXT))
        AND (CAST(:leaseId  AS TEXT) IS NULL OR lease_id  = CAST(:leaseId  AS TEXT))
      GROUP BY payment_category
      """, nativeQuery = true)
  List<Object[]> countByCategory(
      @Param("orgId") String orgId,
      @Param("start") String start,
      @Param("end") String end,
      @Param("tenantId") String tenantId,
      @Param("leaseId") String leaseId);

  // ──────────────────── Due-Soon ───────────────────────────────────────

  /**
   * Payments with a {@code payment_date} falling within the next
   * {@code days} days that are still awaiting processing.
   */
  @Query("SELECT p FROM Payment p " +
      "WHERE p.organizationId = :orgId " +
      "AND p.paymentDate BETWEEN :today AND :maxDate " +
      "AND p.status IN ('PENDING', 'SCHEDULED') " +
      "ORDER BY p.paymentDate ASC")
  List<Payment> findPaymentsDueSoon(
      @Param("orgId") String orgId,
      @Param("today") LocalDate today,
      @Param("maxDate") LocalDate maxDate);

  // ──────────────────── Failed-Retry ──────────────────────────────────

  /**
   * Organisation-scoped list of failed payments eligible for retry.
   */
  @Query("SELECT p FROM Payment p " +
      "WHERE p.status = 'FAILED' " +
      "AND p.organizationId = :orgId " +
      "ORDER BY p.createdAt DESC")
  List<Payment> findFailedPaymentsForRetryByOrg(@Param("orgId") String orgId);

  // ──────────────────── Recurring-Due ─────────────────────────────────

  /**
   * Recurring payments (type = RECURRING_FEE) whose due date is on or
   * before the supplied date and which have not yet been paid.
   */
  @Query("SELECT p FROM Payment p " +
      "WHERE p.organizationId = :orgId " +
      "AND p.paymentType = 'RECURRING_FEE' " +
      "AND p.paymentDate <= :dueDate " +
      "AND p.status IN ('PENDING', 'SCHEDULED') " +
      "ORDER BY p.paymentDate ASC")
  List<Payment> findRecurringPaymentsDue(
      @Param("orgId") String orgId,
      @Param("dueDate") LocalDate dueDate);

  // ──────────────────── GDPR & Data Retention ──────────────────────────

  /**
   * Find payments older than cutoff date (for data retention cleanup)
   * Used by DataRetentionTask to purge data after 3 years
   */
  List<Payment> findByUpdatedAtBefore(LocalDateTime cutoffDate);

  /**
   * Find soft-deleted payments older than cutoff (for final purge)
   */
  @Query("SELECT p FROM Payment p WHERE p.deletedAt IS NOT NULL AND p.deletedAt < :cutoffDate")
  List<Payment> findSoftDeletedBefore(@Param("cutoffDate") LocalDateTime cutoffDate);

  /**
   * Find all payments for organization (no pagination) - for GDPR/bulk operations
   */
  @Query("SELECT p FROM Payment p WHERE p.organizationId = :orgId")
  List<Payment> findAllByOrganizationId(@Param("orgId") String orgId);

  /**
   * Find payments by owner ID
   */
  List<Payment> findByOwnerId(Long ownerId);
}
