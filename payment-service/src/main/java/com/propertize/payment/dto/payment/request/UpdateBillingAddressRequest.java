package com.propertize.payment.dto.payment.request;

import lombok.Data;

@Data
public class UpdateBillingAddressRequest {
    private String line1;
    private String line2;
    private String city;
    private String state;
    private String postalCode;
    private String country;
}
