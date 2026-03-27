package com.propertize.payroll.entity.embedded;

import jakarta.persistence.Column;
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

    @Column(name = "emergency_contact_relationship", length = 50)
    private String relationship;

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
