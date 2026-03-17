package com.propertize.platform.employecraft.entity.embedded;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Embeddable emergency contact information for Employee
 */
@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmergencyContact {

    @Column(name = "emergency_contact_name")
    private String name;

    @Column(name = "emergency_contact_relationship")
    private String relationship;

    @Column(name = "emergency_contact_phone")
    private String phone;

    @Column(name = "emergency_contact_email")
    private String email;

    @Column(name = "emergency_contact_address")
    private String address;
}
