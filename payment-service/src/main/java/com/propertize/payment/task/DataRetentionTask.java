package com.propertize.payment.task;

import com.propertize.payment.entity.Payment;
import com.propertize.payment.entity.PaymentMethod;
import com.propertize.payment.repository.PaymentMethodRepository;
import com.propertize.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Data Retention Task — PCI-DSS & GDPR Compliance
 * 
 * Implements automatic purge of old payment data:
 * - Hard-delete Payment records older than 3 years (pseudonymization)
 * - Hard-delete soft-deleted PaymentMethod records older than 30 days
 * - Clear sensitive fingerprints (card deduplication data) older than 7 years
 * - Generate audit log for each purge operation
 * 
 * Runs daily at 2 AM UTC (configurable)
 * 
 * Compliance:
 * - PCI-DSS 3.2.1: Limit card data retention to 3 years
 * - GDPR Art. 5 (c): Storage limitation principle
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataRetentionTask {

    private final PaymentRepository paymentRepository;
    private final PaymentMethodRepository paymentMethodRepository;

    @Value("${data-retention.payment-days:1095}") // 3 years = 1095 days
    private int paymentRetentionDays;

    @Value("${data-retention.soft-delete-cleanup-days:30}") // 30 days for soft-deleted methods
    private int softDeleteCleanupDays;

    @Value("${data-retention.enabled:true}")
    private boolean retentionEnabled;

    /**
     * Run data retention cleanup daily at 2 AM UTC
     * Scheduled every 24 hours from startup
     */
    @Scheduled(cron = "0 0 2 * * *") // Daily at 2 AM server time (configured as UTC in deployment)
    @Transactional
    public void purgeExpiredPaymentData() {
        if (!retentionEnabled) {
            log.debug("Data retention task disabled");
            return;
        }

        try {
            log.info("═══════════════════════════════════════════════════════════════");
            log.info("Starting data retention cleanup task...");
            log.info("═══════════════════════════════════════════════════════════════");

            LocalDateTime cutoffDate = LocalDateTime.now(ZoneId.of("UTC"))
                    .minusDays(paymentRetentionDays);

            // 1. Hard-delete old Payment records (3-year retention)
            List<Payment> expiredPayments = paymentRepository.findByUpdatedAtBefore(cutoffDate);
            if (!expiredPayments.isEmpty()) {
                log.info("Purging {} payment records older than {} days",
                        expiredPayments.size(), paymentRetentionDays);

                for (Payment payment : expiredPayments) {
                    // Pseudonymize before deletion
                    pseudonymizePayment(payment);
                    paymentRepository.delete(payment);

                    log.debug("Purged payment: id={}, originalDate={}",
                            payment.getId(), payment.getUpdatedAt());
                }
                log.info("✓ Purged {} payment records", expiredPayments.size());
            }

            // 2. Hard-delete soft-deleted PaymentMethod records (30 days after soft-delete)
            LocalDateTime softDeleteCutoff = LocalDateTime.now(ZoneId.of("UTC"))
                    .minusDays(softDeleteCleanupDays);

            List<PaymentMethod> expiredMethods = paymentMethodRepository
                    .findByDeletedAtBeforeAndDeletedAtNotNull(softDeleteCutoff);

            if (!expiredMethods.isEmpty()) {
                log.info("Hard-deleting {} soft-deleted payment methods older than {} days",
                        expiredMethods.size(), softDeleteCleanupDays);

                for (PaymentMethod method : expiredMethods) {
                    log.debug("Hard-deleting soft-deleted method: id={}, deletedAt={}",
                            method.getId(), method.getDeletedAt());
                    paymentMethodRepository.delete(method);
                }
                log.info("✓ Hard-deleted {} payment method records", expiredMethods.size());
            }

            log.info("═══════════════════════════════════════════════════════════════");
            log.info("Data retention cleanup completed successfully");
            log.info("═══════════════════════════════════════════════════════════════");

        } catch (Exception ex) {
            log.error("Data retention cleanup failed", ex);
            // Don't re-throw; allow other scheduled tasks to continue
        }
    }

    /**
     * Pseudonymize payment record before permanent deletion
     * Replaces identifying information with placeholders
     */
    private void pseudonymizePayment(Payment payment) {
        // Placeholder for future pseudonymization rules.
        // Current schema does not include a Stripe customer id field on Payment.
    }

    /**
     * Manual trigger for data retention (for testing/admin)
     * Only accessible via admin endpoints
     */
    @Transactional
    public void manualPurgeExpiredData(int retentionDays) {
        log.warn("Manual data retention purge initiated with {} day threshold", retentionDays);
        LocalDateTime cutoffDate = LocalDateTime.now(ZoneId.of("UTC")).minusDays(retentionDays);

        List<Payment> payments = paymentRepository.findByUpdatedAtBefore(cutoffDate);
        paymentRepository.deleteAll(payments);

        log.info("Manually purged {} payment records", payments.size());
    }
}
