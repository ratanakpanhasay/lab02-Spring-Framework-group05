
package edu.itc.shop.domain;

import java.math.BigDecimal;

public record Money(BigDecimal amount, String currency) {

    public static Money parse(String value) {
        if (value == null || !value.matches("\\d+(\\.\\d{1,2})? [A-Z]{3}")) {
            throw new IllegalArgumentException(
                "Money must use the format '12.50 EUR'"
            );
        }

        String[] parts = value.split(" ");

        return new Money(
            new BigDecimal(parts[0]),
            parts[1]
        );
    }

    @Override
    public String toString() {
        return amount.toPlainString() + " " + currency;
    }
}
