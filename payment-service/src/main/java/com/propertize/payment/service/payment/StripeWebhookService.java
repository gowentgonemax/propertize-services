package com.propertize.payment.service.payment;

import com.propertize.payment.config.StripeConfig;
import com.propertize.payment.entity.Payment;
import com.propertize.payment.entity.TransactionHistory;
import com.propertize.commons.enums.payment.PaymentStatusEnum;
import com.propertize.commons.enums.payment.TransactionStatusEnum;
import com.propertize.commons.enums.payment.TransactionTypeEnum;
import com.propertize.payment.repository.PaymentRepository;
import com.propertize.payment.repository.TransactionHistoryRepository;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.*;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookService {

    private final StripeConfig stripeConfig;
    private final PaymentRepository paymentRepository;
    private final TransactionHistoryRepository transactionHistoryRepository;

    public Event constructEvent(String payload, String sigHeader) {
        try {
            return Webhook.constructEvent(payload, sigHeader, stripeConfig.getWebhookSecret());
        } catch (SignatureVerificationException e) {
            log.error("Stripe webhook signature verification failed: {}", e.getMessage());
            throw new RuntimeException("Invalid Stripe webhook signature", e);
        }
    }

    @Transactional
    public void handleEvent(Event event) {
        log.info("Processing Stripe webhook event: {} ({})", event.getType(), event.getId());

        switch (event.getType()) {
            case "payment_intent.succeeded" -> handlePaymentIntentSucceeded(event);
            case "payment_intent.payment_failed" -> handlePaymentIntentFailed(event);
            case "charge.refunded" -> handleChargeRefunded(event);
            case "payment_method.attached" -> log.debug("PaymentMethod attached: {}", event.getId());
            case "payment_method.detached" -> log.debug("PaymentMethod detached: {}", event.getId());
            default -> log.debug("Unhandled Stripe event type: {}", event.getType());
        }
    }

    private void handlePaymentIntentSucceeded(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        if (deserializer.getObject().isEmpty()) {
            log.warn("No object in payment_intent.succeeded event {}", event.getId());
            return;
        }
        PaymentIntent paymentIntent = (PaymentIntent) deserializer.getObject().get();

        paymentRepository.findByStripePaymentIntentId(paymentIntent.getId()).ifPresent(payment -> {
            payment.setStatus(PaymentStatusEnum.COMPLETED);
            payment.setStripeChargeId(paymentIntent.getLatestCharge());
            paymentRepository.save(payment);
            log.info("Payment {} marked COMPLETED via webhook", payment.getId());
        });
    }

    private void handlePaymentIntentFailed(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        if (deserializer.getObject().isEmpty())
            return;
        PaymentIntent paymentIntent = (PaymentIntent) deserializer.getObject().get();

        paymentRepository.findByStripePaymentIntentId(paymentIntent.getId()).ifPresent(payment -> {
            payment.setStatus(PaymentStatusEnum.FAILED);
            if (paymentIntent.getLastPaymentError() != null) {
                payment.setFailureReason(paymentIntent.getLastPaymentError().getMessage());
            }
            paymentRepository.save(payment);
            log.info("Payment {} marked FAILED via webhook", payment.getId());
        });
    }

    private void handleChargeRefunded(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        if (deserializer.getObject().isEmpty())
            return;
        Charge charge = (Charge) deserializer.getObject().get();

        paymentRepository.findByStripeChargeId(charge.getId()).ifPresent(payment -> {
            payment.setStatus(PaymentStatusEnum.REFUNDED);
            paymentRepository.save(payment);
            log.info("Payment {} marked REFUNDED via webhook", payment.getId());
        });
    }
}
