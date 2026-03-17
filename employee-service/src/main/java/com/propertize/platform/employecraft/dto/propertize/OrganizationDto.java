package com.propertize.platform.employecraft.dto.propertize;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Organization DTO from Propertize
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationDto {
    private UUID id;
    private String name;
    private String email;
    private String phone;
    private String status;
    private String subscriptionTier;
}
