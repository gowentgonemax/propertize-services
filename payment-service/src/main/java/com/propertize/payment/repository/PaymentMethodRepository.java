package com.propertize.payment.repository;

import com.propertize.payment.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, String> {

    List<PaymentMethod> findByTenantIdAndIsActiveTrue(String tenantId);

    List<PaymentMethod> findByUserIdAndIsActiveTrue(Long userId);

    Optional<PaymentMethod> findByStripePaymentMethodId(String stripeId);

    Optional<PaymentMethod> findByFingerprintAndOrganizationId(String fingerprint, String orgId);

    Optional<PaymentMethod> findByTenantIdAndIsDefaultTrue(String tenantId);

    Optional<PaymentMethod> findByUserIdAndIsDefaultTrue(Long userId);

    @Query("SELECT pm FROM PaymentMethod pm WHERE pm.expYear = :year AND pm.expMonth = :month AND pm.isActive = true")
    List<PaymentMethod> findExpiringCards(@Param("year") int year, @Param("month") int month);

    // ──────────────────── GDPR & Data Retention ──────────────────────────

    /**
     * Find soft-deleted payment methods older than cutoff (for hard delete)
     */
    List<PaymentMethod> findByDeletedAtBeforeAndDeletedAtNotNull(LocalDateTime cutoffDate);

    /**
     * Find payment methods by organization (for GDPR export)
     */
    List<PaymentMethod> findByOrganizationId(UUID organizationId);

    /**
     * Find payment methods by organization (string ID variant)
     */
    List<PaymentMethod> findByOrganizationId(String organizationId);
}
