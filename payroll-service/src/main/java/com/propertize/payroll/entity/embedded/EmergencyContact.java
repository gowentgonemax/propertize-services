package com.propertize.payroll.entity.embedded;

import com.propertize.commons.enums.common.RelationshipTypeEnum;
import com.propertize.commons.enums.common.RelationshipTypeEnumConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Embeddable value object representing emergency contact information.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class EmergencyContact {

    @Column(name = "emergency_contact_name", length = 200)
    private String contactName;

    @Convert(converter = RelationshipTypeEnumConverter.class)
    @Column(name = "emergency_contact_relationship", length = 50)
    private RelationshipTypeEnum relationship;

    @Column(name = "emergency_contact_phone", length = 20)
    private String phone;

    @Column(name = "emergency_contact_phone_alt", length = 20)
    private String alternatePhone;

    @Column(name = "emergency_contact_email", length = 100)
    private String email;

    /**
     * Checks if emergency contact is complete.
     */
    public boolean isComplete() {
        return contactName != null && !contactName.isEmpty() &&
                phone != null && !phone.isEmpty();
    }
}
