package com.propertize.payment.service;

import com.propertize.payment.dto.payment.request.PaymentProcessRequest;
import com.propertize.payment.dto.payment.request.PaymentRefundRequest;
import com.propertize.payment.dto.payment.response.StripePaymentIntentResponse;
import com.propertize.payment.dto.payment.response.StripeRefundResponse;
import com.propertize.payment.entity.Payment;

public interface PaymentGatewayService {
    StripePaymentIntentResponse processPayment(Payment payment, PaymentProcessRequest request);

    StripeRefundResponse refundPayment(String transactionId, PaymentRefundRequest request);

    StripePaymentIntentResponse verifyPaymentStatus(String paymentIntentId);

    boolean isGatewayAvailable();
}
