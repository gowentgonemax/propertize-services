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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }
}
