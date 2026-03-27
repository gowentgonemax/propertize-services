package com.propertize.payroll.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO for Organization data from Propertize microservice
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationDto {

    private UUID id;
    private String organizationCode;
    private String name;
    private String legalName;
    private String taxId;
    private String industry;
    private String status;
    private AddressDto address;
    private ContactDto contact;
    private SettingsDto settings;
    private String createdAt;
    private String updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressDto {
        private String street;
        private String street2;
        private String city;
        private String state;
        private String zipCode;
        private String country;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContactDto {
        private String email;
        private String phone;
        private String website;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SettingsDto {
        private String timezone;
        private String dateFormat;
        private String currency;
        private String fiscalYearStart;
        private Boolean multiLocationEnabled;
        private Boolean payrollEnabled;
    }
}
