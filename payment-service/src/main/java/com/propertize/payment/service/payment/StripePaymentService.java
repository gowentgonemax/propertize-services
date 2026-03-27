package com.propertize.payment.service.payment;

import com.propertize.payment.config.StripeConfig;
import com.propertize.payment.dto.payment.request.*;
import com.propertize.payment.dto.payment.response.*;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.param.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripePaymentService {

    private final StripeConfig stripeConfig;

    public StripePaymentIntentResponse createPaymentIntent(StripePaymentIntentRequest request) {
        try {
            PaymentIntentCreateParams.Builder params = PaymentIntentCreateParams.builder()
                    .setAmount(request.getAmount().multiply(BigDecimal.valueOf(100)).longValue())
                    .setCurrency(stripeConfig.getCurrency())
                    .setPaymentMethod(request.getPaymentMethodId())
                    .setConfirmationMethod(PaymentIntentCreateParams.ConfirmationMethod.MANUAL)
                    .setConfirm(false);

            if (request.getDescription() != null) {
                params.setDescription(request.getDescription());
            }
            if (request.getMetadata() != null) {
                params.putAllMetadata(request.getMetadata());
            }
            if (!request.isAutomaticCapture()) {
                params.setCaptureMethod(PaymentIntentCreateParams.CaptureMethod.MANUAL);
            }

            PaymentIntent intent = PaymentIntent.create(params.build());
            return mapToResponse(intent);
        } catch (StripeException e) {
            log.error("Failed to create Stripe PaymentIntent: {}", e.getMessage());
            throw new RuntimeException("Stripe payment intent creation failed: " + e.getMessage(), e);
        }
    }

    public StripePaymentIntentResponse confirmPaymentIntent(String paymentIntentId, String paymentMethodId) {
        try {
            PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
            PaymentIntentConfirmParams.Builder params = PaymentIntentConfirmParams.builder();
            if (paymentMethodId != null) {
                params.setPaymentMethod(paymentMethodId);
            }
            PaymentIntent confirmed = intent.confirm(params.build());
            return mapToResponse(confirmed);
        } catch (StripeException e) {
            log.error("Failed to confirm PaymentIntent {}: {}", paymentIntentId, e.getMessage());
            throw new RuntimeException("Stripe confirm failed: " + e.getMessage(), e);
        }
    }

    public StripePaymentIntentResponse capturePaymentIntent(String paymentIntentId) {
        try {
            PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
            PaymentIntent captured = intent.capture();
            return mapToResponse(captured);
        } catch (StripeException e) {
            log.error("Failed to capture PaymentIntent {}: {}", paymentIntentId, e.getMessage());
            throw new RuntimeException("Stripe capture failed: " + e.getMessage(), e);
        }
    }

    public StripePaymentIntentResponse cancelPaymentIntent(String paymentIntentId) {
        try {
            PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
            PaymentIntent canceled = intent.cancel();
            return mapToResponse(canceled);
        } catch (StripeException e) {
            log.error("Failed to cancel PaymentIntent {}: {}", paymentIntentId, e.getMessage());
            throw new RuntimeException("Stripe cancel failed: " + e.getMessage(), e);
        }
    }

    public StripeRefundResponse createRefund(StripeRefundRequest request) {
        try {
            RefundCreateParams.Builder params = RefundCreateParams.builder();
            if (request.getPaymentIntentId() != null) {
                params.setPaymentIntent(request.getPaymentIntentId());
            } else if (request.getChargeId() != null) {
                params.setCharge(request.getChargeId());
            }
            if (request.getAmount() != null) {
                params.setAmount(request.getAmount().multiply(BigDecimal.valueOf(100)).longValue());
            }
            if (request.getReason() != null) {
                params.setReason(RefundCreateParams.Reason.valueOf(request.getReason().toUpperCase()));
            }

            Refund refund = Refund.create(params.build());

            StripeRefundResponse response = new StripeRefundResponse();
            response.setId(refund.getId());
            response.setAmount(refund.getAmount());
            response.setCurrency(refund.getCurrency());
            response.setStatus(refund.getStatus());
            response.setChargeId(refund.getCharge());
            response.setPaymentIntentId(refund.getPaymentIntent());
            response.setReason(refund.getReason());
            response.setCreated(refund.getCreated());
            return response;
        } catch (StripeException e) {
            log.error("Failed to create Stripe refund: {}", e.getMessage());
            throw new RuntimeException("Stripe refund failed: " + e.getMessage(), e);
        }
    }

    public StripeCustomerResponse createCustomer(StripeCustomerRequest request) {
        try {
            CustomerCreateParams.Builder params = CustomerCreateParams.builder()
                    .setEmail(request.getEmail())
                    .setName(request.getName());
            if (request.getPhone() != null)
                params.setPhone(request.getPhone());
            if (request.getMetadata() != null)
                params.putAllMetadata(request.getMetadata());

            Customer customer = Customer.create(params.build());

            StripeCustomerResponse response = new StripeCustomerResponse();
            response.setId(customer.getId());
            response.setEmail(customer.getEmail());
            response.setName(customer.getName());
            response.setPhone(customer.getPhone());
            response.setCreated(customer.getCreated());
            return response;
        } catch (StripeException e) {
            log.error("Failed to create Stripe customer: {}", e.getMessage());
            throw new RuntimeException("Stripe customer creation failed: " + e.getMessage(), e);
        }
    }

    public void attachPaymentMethod(String paymentMethodId, String customerId) {
        try {
            com.stripe.model.PaymentMethod pm = com.stripe.model.PaymentMethod.retrieve(paymentMethodId);
            PaymentMethodAttachParams params = PaymentMethodAttachParams.builder()
                    .setCustomer(customerId)
                    .build();
            pm.attach(params);
        } catch (StripeException e) {
            log.error("Failed to attach payment method {} to customer {}: {}", paymentMethodId, customerId,
                    e.getMessage());
            throw new RuntimeException("Stripe attach payment method failed: " + e.getMessage(), e);
        }
    }

    public void detachPaymentMethod(String paymentMethodId) {
        try {
            com.stripe.model.PaymentMethod pm = com.stripe.model.PaymentMethod.retrieve(paymentMethodId);
            pm.detach();
        } catch (StripeException e) {
            log.error("Failed to detach payment method {}: {}", paymentMethodId, e.getMessage());
            throw new RuntimeException("Stripe detach payment method failed: " + e.getMessage(), e);
        }
    }

    public StripePaymentIntentResponse retrievePaymentIntent(String paymentIntentId) {
        try {
            PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
            return mapToResponse(intent);
        } catch (StripeException e) {
            log.error("Failed to retrieve PaymentIntent {}: {}", paymentIntentId, e.getMessage());
            throw new RuntimeException("Stripe retrieve failed: " + e.getMessage(), e);
        }
    }

    private StripePaymentIntentResponse mapToResponse(PaymentIntent intent) {
        StripePaymentIntentResponse response = new StripePaymentIntentResponse();
        response.setId(intent.getId());
        response.setClientSecret(intent.getClientSecret());
        response.setStatus(intent.getStatus());
        response.setAmount(intent.getAmount());
        response.setCurrency(intent.getCurrency());
        response.setPaymentMethodId(intent.getPaymentMethod());
        response.setCustomerId(intent.getCustomer());
        response.setDescription(intent.getDescription());
        return response;
    }
}
