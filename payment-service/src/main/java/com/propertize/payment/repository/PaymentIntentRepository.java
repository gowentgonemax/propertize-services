package com.propertize.payment.repository;

import com.propertize.payment.entity.PaymentIntent;
import com.propertize.payment.enums.PaymentIntentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentIntentRepository extends JpaRepository<PaymentIntent, String> {

    Optional<PaymentIntent> findByStripePaymentIntentId(String stripeId);

    Optional<PaymentIntent> findByConfirmationNumber(String confirmationNumber);

    List<PaymentIntent> findByOrganizationIdAndStatus(String orgId, PaymentIntentStatus status);

    @Query("SELECT pi FROM PaymentIntent pi WHERE pi.expiresAt < :now AND pi.status NOT IN ('SUCCEEDED','CANCELLED','FAILED')")
    List<PaymentIntent> findExpiredPaymentIntents(@Param("now") LocalDateTime now);

    @Query("SELECT pi FROM PaymentIntent pi WHERE pi.status = 'FAILED' AND pi.retryCount < 3")
    List<PaymentIntent> findRetryablePaymentIntents();
}
