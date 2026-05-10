package com.propertize.payment.repository.projection;

import java.math.BigDecimal;

/**
 * Projection interface for native SQL payment statistics aggregation query.
 * Each getter maps to the aliased column name returned by the query.
 */
public interface PaymentStatisticsProjection {

    Long getTotalPayments();

    Long getCompletedPayments();

    Long getFailedPayments();

    Long getPendingPayments();

    Long getRefundedPayments();

    BigDecimal getTotalAmount();

    BigDecimal getCompletedAmount();

    BigDecimal getPendingAmount();

    BigDecimal getFailedAmount();

    BigDecimal getRefundedAmount();

    BigDecimal getAverageAmount();
}
