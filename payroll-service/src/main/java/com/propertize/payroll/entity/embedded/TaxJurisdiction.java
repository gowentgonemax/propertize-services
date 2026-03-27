package com.propertize.payroll.entity.embedded;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Embeddable value object representing tax jurisdiction information.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class TaxJurisdiction {

    @Column(name = "federal_ein", length = 20)
    private String federalEin;

    @Column(name = "state_code", length = 2)
    private String stateCode;

    @Column(name = "state_ein", length = 20)
    private String stateEin;

    @Column(name = "local_jurisdiction", length = 100)
    private String localJurisdiction;

    @Column(name = "local_ein", length = 20)
    private String localEin;

    @Column(name = "sui_rate", precision = 5, scale = 4)
    private BigDecimal suiRate;

    @Column(name = "sui_account_number", length = 30)
    private String suiAccountNumber;

    @Column(name = "workers_comp_code", length = 10)
    private String workersCompCode;

    @Column(name = "workers_comp_rate", precision = 5, scale = 4)
    private BigDecimal workersCompRate;
}
