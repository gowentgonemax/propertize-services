package com.propertize.payroll.entity.embedded;

import com.propertize.commons.enums.common.AddressTypeEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Embeddable value object representing a physical address.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class Address {

    @Column(name = "address_street")
    private String street;

    @Column(name = "address_city")
    private String city;

    @Column(name = "address_state", length = 50)
    private String state;

    @Column(name = "address_zip_code", length = 20)
    private String zipCode;

    @Column(name = "address_country", length = 50)
    private String country = "USA";

    @Enumerated(EnumType.STRING)
    @Column(name = "address_type", length = 20)
    private AddressTypeEnum addressType = AddressTypeEnum.PRIMARY;

    /**
     * Returns a formatted single-line address string.
     */
    public String getFormattedAddress() {
        StringBuilder sb = new StringBuilder();
        if (street != null)
            sb.append(street).append(", ");
        if (city != null)
            sb.append(city).append(", ");
        if (state != null)
            sb.append(state).append(" ");
        if (zipCode != null)
            sb.append(zipCode).append(", ");
        if (country != null)
            sb.append(country);
        return sb.toString().replaceAll(", $", "");
    }
}
