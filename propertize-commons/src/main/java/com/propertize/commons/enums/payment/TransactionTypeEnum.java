package com.propertize.commons.enums.payment;

import lombok.Getter;

/**
 * Canonical transaction type enum shared across all Propertize services.
 *
 * <p>
 * Replaces duplicate enums in {@code propertize} and {@code payment-service}.
 */
@Getter
public enum TransactionTypeEnum {
    // Application Fees
    APPLICATION_FEE("Application Fee", "Fee for rental or organization application"),
    APPLICATION_FEE_REFUND("Application Fee Refund", "Refund of application fee"),

    // Rent Payments
    RENT_PAYMENT("Rent Payment", "Monthly rent payment"),
    RENT_PARTIAL("Partial Rent Payment", "Partial rent payment"),
    RENT_ADVANCE("Rent Advance", "Advance rent payment"),

    // Deposits
    SECURITY_DEPOSIT("Security Deposit", "Security deposit payment"),
    SECURITY_DEPOSIT_REFUND("Security Deposit Refund", "Security deposit refund"),
    PET_DEPOSIT("Pet Deposit", "Pet deposit payment"),
    PET_DEPOSIT_REFUND("Pet Deposit Refund", "Pet deposit refund"),

    // Fees
    LATE_FEE("Late Fee", "Late payment fee"),
    LATE_FEE_WAIVED("Late Fee Waived", "Late fee waiver"),
    PROCESSING_FEE("Processing Fee", "Payment processing fee"),
    NSF_FEE("NSF Fee", "Non-sufficient funds fee"),

    // Utilities
    UTILITY_PAYMENT("Utility Payment", "Utility bill payment"),

    // Maintenance
    MAINTENANCE_CHARGE("Maintenance Charge", "Maintenance expense charged to tenant"),

    // Refunds
    REFUND("Refund", "General refund"),
    ADJUSTMENT("Adjustment", "Account adjustment"),

    // Subscriptions (Platform)
    SUBSCRIPTION_PAYMENT("Subscription Payment", "Platform subscription payment"),
    SUBSCRIPTION_REFUND("Subscription Refund", "Platform subscription refund"),

    // Payouts
    OWNER_PAYOUT("Owner Payout", "Payment to property owner"),
    VENDOR_PAYMENT("Vendor Payment", "Payment to vendor"),

    // Other
    OTHER("Other", "Other transaction type");

    private final String displayName;
    private final String description;

    TransactionTypeEnum(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /** Check if this transaction is a credit (money in). */
    public boolean isCredit() {
        return switch (this) {
            case APPLICATION_FEE, RENT_PAYMENT, RENT_PARTIAL, RENT_ADVANCE,
                    SECURITY_DEPOSIT, PET_DEPOSIT, LATE_FEE, PROCESSING_FEE,
                    NSF_FEE, UTILITY_PAYMENT, MAINTENANCE_CHARGE, SUBSCRIPTION_PAYMENT ->
                true;
            default -> false;
        };
    }

    /** Check if this transaction is a debit (money out). */
    public boolean isDebit() {
        return switch (this) {
            case APPLICATION_FEE_REFUND, SECURITY_DEPOSIT_REFUND, PET_DEPOSIT_REFUND,
                    LATE_FEE_WAIVED, REFUND, ADJUSTMENT, SUBSCRIPTION_REFUND,
                    OWNER_PAYOUT, VENDOR_PAYMENT ->
                true;
            default -> false;
        };
    }

    /** Check if this transaction type is related to tenant payments. */
    public boolean isTenantRelated() {
        return switch (this) {
            case APPLICATION_FEE, APPLICATION_FEE_REFUND, RENT_PAYMENT, RENT_PARTIAL,
                    RENT_ADVANCE, SECURITY_DEPOSIT, SECURITY_DEPOSIT_REFUND, PET_DEPOSIT,
                    PET_DEPOSIT_REFUND, LATE_FEE, LATE_FEE_WAIVED, UTILITY_PAYMENT,
                    MAINTENANCE_CHARGE, REFUND ->
                true;
            default -> false;
        };
    }
}
