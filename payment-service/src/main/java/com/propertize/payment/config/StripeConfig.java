package com.propertize.payment.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class StripeConfig {

    @Value("${payment.stripe.api-key}")
    private String apiKey;

    @Value("${payment.stripe.webhook-secret}")
    private String webhookSecret;

    @Value("${payment.stripe.publishable-key:}")
    private String publishableKey;

    @Value("${payment.stripe.currency:usd}")
    private String currency;

    @PostConstruct
    public void init() {
        Stripe.apiKey = this.apiKey;
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public boolean isTestMode() {
        return apiKey != null && apiKey.startsWith("sk_test_");
    }

    public boolean isLiveMode() {
        return apiKey != null && apiKey.startsWith("sk_live_");
    }
}
