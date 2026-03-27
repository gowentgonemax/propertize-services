package com.propertize.payment.dto.payment.request;

import lombok.Data;
import java.util.Map;

@Data
public class StripeCustomerRequest {
    private String email;
    private String name;
    private String phone;
    private String organizationId;
    private String tenantId;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private Map<String, String> metadata;
}
