package com.propertize.payroll.entity.embedded;

import com.propertize.commons.enums.payment.BankAccountTypeEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Embeddable value object representing banking/payment information.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class BankingInfo {

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "bank_routing_number", length = 20)
    private String routingNumber;

    @Column(name = "bank_account_number", length = 30)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "bank_account_type", length = 20)
    private BankAccountTypeEnum accountType;

    @Column(name = "bank_account_holder_name")
    private String accountHolderName;

    /**
     * Returns a masked account number for display purposes.
     */
    public String getMaskedAccountNumber() {
        if (accountNumber == null || accountNumber.length() <= 4) {
            return "****";
        }
        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }

    /**
     * Returns a masked routing number for display purposes.
     */
    public String getMaskedRoutingNumber() {
        if (routingNumber == null || routingNumber.length() <= 4) {
            return "****";
        }
        return "****" + routingNumber.substring(routingNumber.length() - 4);
    }
}
