package com.propertize.payment.dto.promo;

import jakarta.validation.constraints.NotBlank;

public class PromoCodeValidateRequest {
    @NotBlank
    private String code;

    @NotBlank
    private String organizationId;

    private String applicationId;
    private String applicantEmail;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public String getApplicantEmail() {
        return applicantEmail;
    }

    public void setApplicantEmail(String applicantEmail) {
        this.applicantEmail = applicantEmail;
    }
}
