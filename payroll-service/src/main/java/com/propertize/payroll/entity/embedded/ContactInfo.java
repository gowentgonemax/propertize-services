package com.propertize.payroll.entity.embedded;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Embeddable value object representing contact information.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class ContactInfo {

    @Column(name = "contact_email")
    private String email;

    @Column(name = "contact_phone", length = 20)
    private String phone;

    @Column(name = "contact_mobile", length = 20)
    private String mobile;

    @Column(name = "contact_fax", length = 20)
    private String fax;

    @Column(name = "contact_person")
    private String contactPerson;

    /**
     * Returns the primary phone number (mobile preferred, otherwise phone).
     */
    public String getPrimaryPhone() {
        return mobile != null && !mobile.isBlank() ? mobile : phone;
    }
}
