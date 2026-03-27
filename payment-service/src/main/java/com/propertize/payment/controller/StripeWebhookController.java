package com.propertize.payment.controller;

import com.propertize.payment.config.ApiVersion;
import com.propertize.payment.dto.common.ApiResponse;
import com.propertize.payment.service.payment.StripeWebhookService;
import com.propertize.payment.util.ResponseHandler;
import com.stripe.model.Event;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiVersion.V1 + "/webhooks/stripe")
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookController {

    private final StripeWebhookService stripeWebhookService;

    /**
     * Stripe webhook endpoint — must be permitted without authentication.
     * Raw body is required for signature verification.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        try {
            Event event = stripeWebhookService.constructEvent(payload, sigHeader);
            stripeWebhookService.handleEvent(event);
            return ResponseHandler.success(null, "Webhook processed");
        } catch (RuntimeException e) {
            log.error("Webhook processing failed: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}
