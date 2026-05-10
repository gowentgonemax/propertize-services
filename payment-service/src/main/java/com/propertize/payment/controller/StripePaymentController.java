package com.propertize.payment.controller;

import com.propertize.payment.config.ApiVersion;
import com.propertize.commons.dto.ApiResponse;
import com.propertize.payment.dto.payment.request.*;
import com.propertize.payment.dto.payment.response.*;
import com.propertize.payment.service.payment.StripePaymentService;
import com.propertize.commons.dto.ResponseHandler;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Stripe Payment Controller — PCI-DSS Rate-Limiting Compliant
 * 
 * Rate limiting applied to payment-critical operations:
 * - Payment intents: 10 req/min (prevents brute force)
 * - General operations: 30 req/min (prevents abuse)
 * 
 * All card data remains tokenized (client-side via Stripe Elements)
 * Server receives only Stripe payment method IDs
 */
@RestController
@RequestMapping(ApiVersion.V1 + "/stripe")
@RequiredArgsConstructor
public class StripePaymentController {

    private final StripePaymentService stripePaymentService;

    @PostMapping("/payment-intents")
    @RateLimiter(name = "payment-operations") // 10 req/min
    public ResponseEntity<ApiResponse<StripePaymentIntentResponse>> createPaymentIntent(
            @Valid @RequestBody StripePaymentIntentRequest request) {
        return ResponseHandler.handleSave(() -> stripePaymentService.createPaymentIntent(request), "PaymentIntent");
    }

    @PostMapping("/payment-intents/{id}/confirm")
    @RateLimiter(name = "payment-operations") // 10 req/min
    public ResponseEntity<ApiResponse<StripePaymentIntentResponse>> confirmPaymentIntent(
            @PathVariable String id,
            @RequestParam(required = false) String paymentMethodId) {
        return ResponseHandler.handleSave(() -> stripePaymentService.confirmPaymentIntent(id, paymentMethodId),
                "PaymentIntent");
    }

    @PostMapping("/payment-intents/{id}/capture")
    @RateLimiter(name = "payment-operations") // 10 req/min
    public ResponseEntity<ApiResponse<StripePaymentIntentResponse>> capturePaymentIntent(@PathVariable String id) {
        return ResponseHandler.handleSave(() -> stripePaymentService.capturePaymentIntent(id), "PaymentIntent");
    }

    @PostMapping("/payment-intents/{id}/cancel")
    @RateLimiter(name = "payment-operations") // 10 req/min
    public ResponseEntity<ApiResponse<StripePaymentIntentResponse>> cancelPaymentIntent(@PathVariable String id) {
        return ResponseHandler.handleSave(() -> stripePaymentService.cancelPaymentIntent(id), "PaymentIntent");
    }

    @PostMapping("/refunds")
    @RateLimiter(name = "payment-operations") // 10 req/min
    public ResponseEntity<ApiResponse<StripeRefundResponse>> createRefund(
            @Valid @RequestBody StripeRefundRequest request) {
        return ResponseHandler.handleSave(() -> stripePaymentService.createRefund(request), "Refund");
    }

    @GetMapping("/payment-intents/{id}")
    @RateLimiter(name = "general-payment") // 30 req/min
    public ResponseEntity<ApiResponse<StripePaymentIntentResponse>> retrievePaymentIntent(@PathVariable String id) {
        return ResponseHandler.handleFind(() -> stripePaymentService.retrievePaymentIntent(id), "PaymentIntent");
    }
}
