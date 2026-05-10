package com.propertize.payment.dto.payment.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Aggregated payment statistics response.
 * <p>
 * Matches the frontend {@code PaymentStatistics} interface in
 * {@code propertize-front-end/src/types/payment.types.ts}.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentStatisticsResponse {

    /** Total number of payments in the filtered range. */
    private long totalPayments;

    /** Count of COMPLETED payments. */
    private long completedPayments;

    /** Count of FAILED payments. */
    private long failedPayments;

    /** Count of PENDING or PROCESSING payments. */
    private long pendingPayments;

    /** Count of REFUNDED / PARTIALLY_REFUNDED payments. */
    private long refundedPayments;

    /** Sum of all payment amounts. */
    private BigDecimal totalAmount;

    /** Sum of COMPLETED payment amounts. */
    private BigDecimal completedAmount;

    /** Sum of PENDING / PROCESSING payment amounts. */
    private BigDecimal pendingAmount;

    /** Sum of FAILED payment amounts. */
    private BigDecimal failedAmount;

    /** Sum of REFUNDED payment amounts. */
    private BigDecimal refundedAmount;

    /** Ratio of completed payments to total (0–100). */
    private double successRate;

    /** Mean payment amount across all filtered payments. */
    private BigDecimal averageAmount;

    /**
     * Aliases for backward-compatibility with frontend fields that use
     * {@code totalCompleted / totalPending / totalRefunded} as count names.
     */
    private long totalCompleted;
    private long totalPending;
    private long totalRefunded;

    /** Counts grouped by payment method name (optional). */
    private Map<String, Long> paymentsByMethod;

    /** Counts grouped by payment category name (optional). */
    private Map<String, Long> paymentsByCategory;

    /**
     * Factory method — assembles the response from the native-query projection
     * plus the optional breakdown maps.
     *
     * @param proj       raw aggregation from the database
     * @param byMethod   count-per-method map (may be null)
     * @param byCategory count-per-category map (may be null)
     */
    public static PaymentStatisticsResponse from(
            com.propertize.payment.repository.projection.PaymentStatisticsProjection proj,
            Map<String, Long> byMethod,
            Map<String, Long> byCategory) {

        long total = proj.getTotalPayments() != null ? proj.getTotalPayments() : 0L;
        long completed = proj.getCompletedPayments() != null ? proj.getCompletedPayments() : 0L;
        long failed = proj.getFailedPayments() != null ? proj.getFailedPayments() : 0L;
        long pending = proj.getPendingPayments() != null ? proj.getPendingPayments() : 0L;
        long refunded = proj.getRefundedPayments() != null ? proj.getRefundedPayments() : 0L;

        BigDecimal totalAmt = nonNull(proj.getTotalAmount());
        BigDecimal completedAmt = nonNull(proj.getCompletedAmount());
        BigDecimal pendingAmt = nonNull(proj.getPendingAmount());
        BigDecimal failedAmt = nonNull(proj.getFailedAmount());
        BigDecimal refundedAmt = nonNull(proj.getRefundedAmount());
        BigDecimal avgAmt = nonNull(proj.getAverageAmount());

        double rate = total > 0
                ? BigDecimal.valueOf(completed)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                        .doubleValue()
                : 0.0;

        return PaymentStatisticsResponse.builder()
                .totalPayments(total)
                .completedPayments(completed)
                .failedPayments(failed)
                .pendingPayments(pending)
                .refundedPayments(refunded)
                .totalAmount(totalAmt)
                .completedAmount(completedAmt)
                .pendingAmount(pendingAmt)
                .failedAmount(failedAmt)
                .refundedAmount(refundedAmt)
                .successRate(rate)
                .averageAmount(avgAmt)
                // aliases expected by frontend
                .totalCompleted(completed)
                .totalPending(pending)
                .totalRefunded(refunded)
                .paymentsByMethod(byMethod)
                .paymentsByCategory(byCategory)
                .build();
    }

    private static BigDecimal nonNull(BigDecimal value) {
        return value != null ? value.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }
}
