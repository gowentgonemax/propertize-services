package com.propertize.payroll.dto.common;

import com.propertize.payroll.enums.AddressTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Common DTO for address representation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddressDto {
    private String street;
    private String city;
    private String state;
    private String zipCode;
    private String country = "USA";
    private AddressTypeEnum addressType = AddressTypeEnum.PRIMARY;

    /**
     * Returns a formatted address string.
     */
    public String getFormattedAddress() {
        StringBuilder sb = new StringBuilder();
        if (street != null) sb.append(street).append(", ");
        if (city != null) sb.append(city).append(", ");
        if (state != null) sb.append(state).append(" ");
        if (zipCode != null) sb.append(zipCode).append(", ");
        if (country != null) sb.append(country);
        return sb.toString().replaceAll(", $", "");
    }
}
