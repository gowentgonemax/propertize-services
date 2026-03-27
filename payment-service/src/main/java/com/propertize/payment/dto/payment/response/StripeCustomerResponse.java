package com.propertize.payment.dto.payment.response;

import lombok.Data;

@Data
public class StripeCustomerResponse {
    private String id;
    private String email;
    private String name;
    private String phone;
    private String defaultPaymentMethod;
    private Long created;
}
