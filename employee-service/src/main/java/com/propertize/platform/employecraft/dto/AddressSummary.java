package com.propertize.platform.employecraft.dto;

import lombok.Builder;

@Builder
public record AddressSummary(
        String streetAddress,
        String city,
        String state,
        String zipCode,
        String country) {
}
