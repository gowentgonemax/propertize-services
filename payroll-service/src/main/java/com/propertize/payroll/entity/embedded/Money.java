package com.propertize.payroll.entity.embedded;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Currency;

/**
 * Embeddable value object representing a monetary amount with currency.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class Money {

    @Column(name = "money_amount", precision = 15, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "money_currency", length = 3)
    private String currencyCode = "USD";

    public Money(BigDecimal amount) {
        this.amount = amount;
        this.currencyCode = "USD";
    }

    public Money(double amount) {
        this.amount = BigDecimal.valueOf(amount);
        this.currencyCode = "USD";
    }

    public Currency getCurrency() {
        return Currency.getInstance(currencyCode);
    }

    public Money add(Money other) {
        if (!this.currencyCode.equals(other.currencyCode)) {
            throw new IllegalArgumentException("Cannot add money with different currencies");
        }
        return new Money(this.amount.add(other.amount), this.currencyCode);
    }

    public Money subtract(Money other) {
        if (!this.currencyCode.equals(other.currencyCode)) {
            throw new IllegalArgumentException("Cannot subtract money with different currencies");
        }
        return new Money(this.amount.subtract(other.amount), this.currencyCode);
    }

    public Money multiply(BigDecimal multiplier) {
        return new Money(this.amount.multiply(multiplier), this.currencyCode);
    }

    public boolean isPositive() {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isNegative() {
        return amount != null && amount.compareTo(BigDecimal.ZERO) < 0;
    }

    public boolean isZero() {
        return amount == null || amount.compareTo(BigDecimal.ZERO) == 0;
    }
}
