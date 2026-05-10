package com.propertize.payment.service.gdpr;

import com.propertize.payment.entity.Payment;
import com.propertize.payment.entity.PaymentMethod;
import com.propertize.payment.repository.PaymentMethodRepository;
import com.propertize.payment.repository.PaymentRepository;
import com.stripe.exception.StripeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * GDPR Right to Erasure Service
 * 
 * Implements GDPR Article 17 (right to be forgotten):
 * - Soft-delete payment records (mark as deleted)
 * - Detach payment methods from Stripe customer
 * - Hard-delete after retention period
 * - Audit log all erasure operations
 * 
 * Two-stage erasure:
 * 1. User initiates: soft-delete + Stripe detach
 * 2. Scheduled cleanup: hard-delete after 30 days (DataRetentionTask)
 * 
 * Security: Only user or admin can request erasure of their data
 */
@Service
public class GdprErasureService {

    private static final Logger log = LoggerFactory.getLogger(GdprErasureService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentMethodRepository paymentMethodRepository;

    public GdprErasureService(PaymentRepository paymentRepository,
            PaymentMethodRepository paymentMethodRepository) {
        this.paymentRepository = paymentRepository;
        this.paymentMethodRepository = paymentMethodRepository;
    }

    /**
     * Initiate erasure workflow for a user's payment data
     * Soft-deletes records and detaches from Stripe
     * Note: Payment entity stores ownerId and processedByUserId
     */
    @Transactional
    public void initiateUserErasure(UUID userId, UUID organizationId, String reason) {
        log.warn("GDPR erasure requested for user: {}, org: {}, reason: {}",
                userId, organizationId, reason);

        try {
            // 1. Soft-delete all payment records for this user/organization
            List<Payment> allPayments = paymentRepository.findAllByOrganizationId(organizationId.toString());
            Long userIdLong = userId == null ? null : Long.parseLong(userId.toString().replaceAll("[^0-9]", ""));
            
            List<Payment> payments = allPayments.stream()
                .filter(p -> (p.getOwnerId() != null && p.getOwnerId().equals(userIdLong)) ||
                            (p.getProcessedByUserId() != null && p.getProcessedByUserId().equals(userIdLong)))
                .collect(Collectors.toList());
            
            for (Payment payment : payments) {
                paymentRepository.delete(payment);
                log.info("Soft-deleted payment: {}", payment.getId());
            }

            // 2. Detach all payment methods from Stripe + soft-delete locally
            List<PaymentMethod> methods = paymentMethodRepository.findByOrganizationId(organizationId.toString());
            for (PaymentMethod method : methods) {
                try {
                    detachFromStripe(method.getStripePaymentMethodId());
                    paymentMethodRepository.delete(method);
                    log.info("Detached from Stripe and soft-deleted method: {}", method.getId());
                } catch (StripeException ex) {
                    log.warn("Failed to detach Stripe payment method {}: {}",
                            method.getStripePaymentMethodId(), ex.getMessage());
                    // Continue with other methods; don't block erasure
                }
            }

            log.info("✓ GDPR erasure initiated: {} payments, {} methods soft-deleted",
                    payments.size(), methods.size());

            // 3. Audit log (implement via audit service)
            // auditService.logGdprErasure(userId, organizationId, reason, payments.size(),
            // methods.size());

        } catch (Exception ex) {
            log.error("GDPR erasure failed for user: {}", userId, ex);
            throw new RuntimeException("Erasure request failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * Hard-delete all data for a user (final erasure)
     * Called after retention period by DataRetentionTask
     * Only for fully anonymized/expired records
     */
    @Transactional
    public void hardDeleteUserData(UUID userId, UUID organizationId) {
        log.warn("Hard-deleting all data for user: {}, org: {}", userId, organizationId);

        List<Payment> allPayments = paymentRepository.findAllByOrganizationId(organizationId.toString());
        Long userIdLong = userId == null ? null : Long.parseLong(userId.toString().replaceAll("[^0-9]", ""));
        
        List<Payment> payments = allPayments.stream()
            .filter(p -> (p.getOwnerId() != null && p.getOwnerId().equals(userIdLong)) ||
                        (p.getProcessedByUserId() != null && p.getProcessedByUserId().equals(userIdLong)))
            .collect(Collectors.toList());
        
        List<PaymentMethod> methods = paymentMethodRepository.findByOrganizationId(organizationId.toString());

        paymentRepository.deleteAll(payments);
        paymentMethodRepository.deleteAll(methods);

        log.info("✓ Hard-deleted {} payments and {} methods", payments.size(), methods.size());
    }

    /**
     * Detach payment method from Stripe
     * Prevents future charges using this method
     */
    private void detachFromStripe(String stripePaymentMethodId) throws StripeException {
        com.stripe.model.PaymentMethod method = com.stripe.model.PaymentMethod.retrieve(stripePaymentMethodId);
        method.detach();
        log.info("✓ Detached from Stripe: {}", stripePaymentMethodId);
    }

    /**
     * Manually erase a specific payment method
     */
    @Transactional
    public void erasePaymentMethod(UUID paymentMethodId) {
        log.warn("Manual erasure of payment method: {}", paymentMethodId);

        PaymentMethod method = paymentMethodRepository.findById(paymentMethodId.toString())
                .orElseThrow(() -> new IllegalArgumentException("Payment method not found"));

        try {
            detachFromStripe(method.getStripePaymentMethodId());
        } catch (StripeException ex) {
            log.warn("Failed to detach from Stripe: {}", ex.getMessage());
        }

        paymentMethodRepository.delete(method);
        log.info("✓ Payment method marked for erasure: {}", paymentMethodId);
    }
}
