package com.propertize.payment.repository;

import com.propertize.payment.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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
}
