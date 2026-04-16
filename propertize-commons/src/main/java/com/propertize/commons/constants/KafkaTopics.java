package com.propertize.commons.constants;

/**
 * Canonical Kafka topic names used across all Propertize services.
 *
 * <p>Centralising topic names in commons prevents "employee-events" being
 * independently re-typed in both the publisher (employee-service) and the
 * consumer (payroll-service), which caused a silent mismatch in the past.
 *
 * <h3>Usage — Producer</h3>
 * <pre>{@code
 * kafkaTemplate.send(KafkaTopics.EMPLOYEE_EVENTS, key, event);
 * }</pre>
 *
 * <h3>Usage — Consumer</h3>
 * <pre>{@code
 * @KafkaListener(topics = KafkaTopics.EMPLOYEE_EVENTS, groupId = "payroll-service")
 * }</pre>
 *
 * <p><strong>NOTE:</strong> Spring {@code @KafkaListener#topics} only accepts
 * compile-time string constants, which is satisfied by {@code public static final String}.
 */
public final class KafkaTopics {

    private KafkaTopics() {
        throw new UnsupportedOperationException("Utility class");
    }

    // ── Employee domain ───────────────────────────────────────────────────────

    /**
     * Employee lifecycle events: CREATED, UPDATED, TERMINATED.
     * Published by: {@code employee-service}
     * Consumed by: {@code payroll-service}
     */
    public static final String EMPLOYEE_EVENTS = "employee-events";

    // ── Payment domain ────────────────────────────────────────────────────────

    /**
     * Payment status change events.
     * Published by: {@code payment-service}
     * Consumed by: {@code propertize} (audit, notifications)
     */
    public static final String PAYMENT_EVENTS = "payment-events";

    // ── Notification domain ───────────────────────────────────────────────────

    /**
     * Notification dispatch requests.
     * Published by: multiple services
     * Consumed by: notification workers / Python services
     */
    public static final String NOTIFICATION_EVENTS = "notification-events";

    // ── Audit domain ──────────────────────────────────────────────────────────

    /**
     * Audit log entries from any service.
     * Published by: any service
     * Consumed by: audit worker / MongoDB sink
     */
    public static final String AUDIT_EVENTS = "audit-events";

    // ── Analytics domain ──────────────────────────────────────────────────────

    /**
     * Analytics / telemetry events.
     * Published by: propertize
     * Consumed by: analytics-worker
     */
    public static final String ANALYTICS_EVENTS = "analytics-events";
}

