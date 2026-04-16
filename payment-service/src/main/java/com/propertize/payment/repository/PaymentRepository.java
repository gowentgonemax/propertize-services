package com.propertize.payment.repository;

import com.propertize.payment.entity.Payment;
import com.propertize.commons.enums.payment.PaymentCategoryEnum;
import com.propertize.commons.enums.payment.PaymentContextEnum;
import com.propertize.commons.enums.payment.PaymentStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {

        Page<Payment> findByOrganizationId(String organizationId, Pageable pageable);

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
}
