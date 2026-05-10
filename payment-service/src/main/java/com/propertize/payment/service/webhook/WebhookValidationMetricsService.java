package com.propertize.payment.service.webhook;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Webhook Validation Metrics — PCI-DSS Monitoring
 * 
 * Tracks webhook signature verification attempts:
 * - Valid signatures: webhook.signature.valid
 * - Invalid signatures: webhook.signature.invalid (suspicious!)
 * - Processing errors: webhook.processing.error
 * 
 * Alerts if invalid signatures exceed 5/minute (indicates tampering or
 * misconfiguration)
 * 
 * Exposed via /actuator/metrics for Prometheus/Grafana integration
 */
@Service
public class WebhookValidationMetricsService {

    private static final Logger log = LoggerFactory.getLogger(WebhookValidationMetricsService.class);

    private final MeterRegistry meterRegistry;

    private Counter validSignatureCounter;
    private Counter invalidSignatureCounter;
    private Counter processingErrorCounter;

    public WebhookValidationMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        initializeMetrics();
    }

    private void initializeMetrics() {
        validSignatureCounter = Counter.builder("webhook.signature.valid")
                .description("Number of valid Stripe webhook signatures")
                .tag("service", "payment")
                .tag("provider", "stripe")
                .register(meterRegistry);

        invalidSignatureCounter = Counter.builder("webhook.signature.invalid")
                .description("Number of invalid Stripe webhook signatures (suspicious)")
                .tag("service", "payment")
                .tag("provider", "stripe")
                .register(meterRegistry);

        processingErrorCounter = Counter.builder("webhook.processing.error")
                .description("Number of webhook processing errors")
                .tag("service", "payment")
                .tag("provider", "stripe")
                .register(meterRegistry);
    }

    /**
     * Record successful webhook signature verification
     */
    public void recordValidSignature(String eventId, String eventType) {
        validSignatureCounter.increment();
        log.debug("✓ Webhook signature valid: event_id={}, type={}", eventId, eventType);
    }

    /**
     * Record failed webhook signature verification
     * ⚠️ Alert if this exceeds 5/minute (possible tampering)
     */
    public void recordInvalidSignature(String eventId, String reason) {
        invalidSignatureCounter.increment();
        double invalidCount = invalidSignatureCounter.count();

        log.warn("✗ Webhook signature INVALID: event_id={}, reason={}, total_invalid={}",
                eventId, reason, invalidCount);

        // Alert if too many failures
        if (invalidCount > 5) {
            log.error("⚠️ ALERT: Invalid webhook signatures exceed threshold ({}). "
                    + "Possible tampering or misconfiguration.", invalidCount);
        }
    }

    /**
     * Record webhook processing error
     */
    public void recordProcessingError(String eventId, String errorMessage) {
        processingErrorCounter.increment();
        log.error("Webhook processing error: event_id={}, error={}", eventId, errorMessage);
    }

    /**
     * Get current metrics for monitoring
     */
    public WebhookMetricsSnapshot getMetricsSnapshot() {
        return new WebhookMetricsSnapshot(
                (int) validSignatureCounter.count(),
                (int) invalidSignatureCounter.count(),
                (int) processingErrorCounter.count());
    }

    /**
     * Snapshot of webhook metrics for admin dashboard
     */
    public static class WebhookMetricsSnapshot {
        private int validSignatures;
        private int invalidSignatures;
        private int processingErrors;

        public WebhookMetricsSnapshot(int validSignatures, int invalidSignatures, int processingErrors) {
            this.validSignatures = validSignatures;
            this.invalidSignatures = invalidSignatures;
            this.processingErrors = processingErrors;
        }

        public int getValidSignatures() {
            return validSignatures;
        }

        public int getInvalidSignatures() {
            return invalidSignatures;
        }

        public int getProcessingErrors() {
            return processingErrors;
        }

        public boolean isHealthy() {
            // Consider unhealthy if invalid > 5 or errors > 10 in any period
            return invalidSignatures <= 5 && processingErrors <= 10;
        }
    }
}
