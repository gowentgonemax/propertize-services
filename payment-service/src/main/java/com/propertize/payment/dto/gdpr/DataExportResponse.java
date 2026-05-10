package com.propertize.payment.dto.gdpr;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * GDPR Data Export Response
 * 
 * JSON structure for Data Subject Access Request (DSAR)
 * Contains all payment-related data for a user/organization
 * Exportable as JSON or CSV
 */
public class DataExportResponse {

    @JsonProperty("requested_at")
    private LocalDateTime requestedAt;

    @JsonProperty("user_id")
    private UUID userId;

    @JsonProperty("organization_id")
    private UUID organizationId;

    @JsonProperty("payment_records")
    private List<PaymentRecord> paymentRecords;

    @JsonProperty("payment_methods")
    private List<PaymentMethodRecord> paymentMethods;

    @JsonProperty("total_records")
    private int totalRecords;

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
    }

    public List<PaymentRecord> getPaymentRecords() {
        return paymentRecords;
    }

    public void setPaymentRecords(List<PaymentRecord> paymentRecords) {
        this.paymentRecords = paymentRecords;
    }

    public List<PaymentMethodRecord> getPaymentMethods() {
        return paymentMethods;
    }

    public void setPaymentMethods(List<PaymentMethodRecord> paymentMethods) {
        this.paymentMethods = paymentMethods;
    }

    public int getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(int totalRecords) {
        this.totalRecords = totalRecords;
    }

    /**
     * Payment record (transaction history)
     */
    public static class PaymentRecord {

        @JsonProperty("id")
        private UUID id;

        @JsonProperty("stripe_payment_intent_id")
        private String stripePaymentIntentId;

        @JsonProperty("amount")
        private BigDecimal amount;

        @JsonProperty("currency")
        private String currency;

        @JsonProperty("status")
        private String status;

        @JsonProperty("created_at")
        private LocalDateTime createdAt;

        @JsonProperty("updated_at")
        private LocalDateTime updatedAt;

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public String getStripePaymentIntentId() {
            return stripePaymentIntentId;
        }

        public void setStripePaymentIntentId(String stripePaymentIntentId) {
            this.stripePaymentIntentId = stripePaymentIntentId;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public LocalDateTime getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
        }
    }

    /**
     * Payment method record (stored payment info)
     */
    public static class PaymentMethodRecord {

        @JsonProperty("id")
        private UUID id;

        @JsonProperty("stripe_payment_method_id")
        private String stripePaymentMethodId;

        @JsonProperty("card_brand")
        private String cardBrand;

        @JsonProperty("last_four")
        private String lastFour;

        @JsonProperty("exp_month")
        private String expMonth;

        @JsonProperty("exp_year")
        private String expYear;

        @JsonProperty("billing_name")
        private String billingName;

        @JsonProperty("created_at")
        private LocalDateTime createdAt;

        @JsonProperty("updated_at")
        private LocalDateTime updatedAt;

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public String getStripePaymentMethodId() {
            return stripePaymentMethodId;
        }

        public void setStripePaymentMethodId(String stripePaymentMethodId) {
            this.stripePaymentMethodId = stripePaymentMethodId;
        }

        public String getCardBrand() {
            return cardBrand;
        }

        public void setCardBrand(String cardBrand) {
            this.cardBrand = cardBrand;
        }

        public String getLastFour() {
            return lastFour;
        }

        public void setLastFour(String lastFour) {
            this.lastFour = lastFour;
        }

        public String getExpMonth() {
            return expMonth;
        }

        public void setExpMonth(String expMonth) {
            this.expMonth = expMonth;
        }

        public String getExpYear() {
            return expYear;
        }

        public void setExpYear(String expYear) {
            this.expYear = expYear;
        }

        public String getBillingName() {
            return billingName;
        }

        public void setBillingName(String billingName) {
            this.billingName = billingName;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public LocalDateTime getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
        }
    }
}
