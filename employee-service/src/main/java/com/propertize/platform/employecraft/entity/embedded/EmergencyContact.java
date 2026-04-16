package com.propertize.platform.employecraft.entity.embedded;

import com.propertize.commons.enums.common.RelationshipTypeEnum;
import com.propertize.commons.enums.common.RelationshipTypeEnumConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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

    @Convert(converter = RelationshipTypeEnumConverter.class)
    @Column(name = "emergency_contact_relationship")
    private RelationshipTypeEnum relationship;

    @Column(name = "emergency_contact_phone")
    private String phone;

    @Column(name = "emergency_contact_email")
    private String email;

    @Column(name = "emergency_contact_address")
    private String address;
}
