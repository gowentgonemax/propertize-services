package com.propertize.payment.dto.payment.request;

import java.math.BigDecimal;

public class ApplicationFeeRequest {
    private String organizationId;
    private String rentalApplicationId;
    private String applicantId;
    private String applicantEmail;
    private BigDecimal feeAmount;
    private String promoCode;
    private String paymentMethodId;
    private String stripePaymentMethodId;
    private String stripeCustomerId;
    private String notes;

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public String getRentalApplicationId() {
        return rentalApplicationId;
    }

    public void setRentalApplicationId(String rentalApplicationId) {
        this.rentalApplicationId = rentalApplicationId;
    }

    public String getApplicantId() {
        return applicantId;
    }

    public void setApplicantId(String applicantId) {
        this.applicantId = applicantId;
    }

    public String getApplicantEmail() {
        return applicantEmail;
    }

    public void setApplicantEmail(String applicantEmail) {
        this.applicantEmail = applicantEmail;
    }

    public BigDecimal getFeeAmount() {
        return feeAmount;
    }

    public void setFeeAmount(BigDecimal feeAmount) {
        this.feeAmount = feeAmount;
    }

    public String getPromoCode() {
        return promoCode;
    }

    public void setPromoCode(String promoCode) {
        this.promoCode = promoCode;
    }

    public String getPaymentMethodId() {
        return paymentMethodId;
    }

    public void setPaymentMethodId(String paymentMethodId) {
        this.paymentMethodId = paymentMethodId;
    }

    public String getStripePaymentMethodId() {
        return stripePaymentMethodId;
    }

    public void setStripePaymentMethodId(String stripePaymentMethodId) {
        this.stripePaymentMethodId = stripePaymentMethodId;
    }

    public String getStripeCustomerId() {
        return stripeCustomerId;
    }

    public void setStripeCustomerId(String stripeCustomerId) {
        this.stripeCustomerId = stripeCustomerId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
