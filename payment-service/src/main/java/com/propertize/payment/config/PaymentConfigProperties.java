package com.propertize.payment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Data
@Component
@ConfigurationProperties(prefix = "payment")
public class PaymentConfigProperties {

    private Stripe stripe = new Stripe();
    private Fees fees = new Fees();
    private Retry retry = new Retry();

    @Data
    public static class Stripe {
        private String apiKey;
        private String webhookSecret;
        private String publishableKey;
        private String currency = "usd";
    }

    @Data
    public static class Fees {
        private BigDecimal applicationFee = new BigDecimal("50.00");
        private BigDecimal organizationOnboardingFee = new BigDecimal("0.00");
        private BigDecimal lateFeePercent = new BigDecimal("5.00");
        private int gracePeriodDays = 5;
    }

    @Data
    public static class Retry {
        private int maxAttempts = 3;
        private long delayMs = 1000;
        private long maxDelayMs = 5000;
    }
}
