package com.example;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Validates payment requests.
 */
public class PaymentValidator {

    private static final BigDecimal MIN_AMOUNT = new BigDecimal("0.01");
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000.00");

    public boolean validateAmount(BigDecimal amount) {
        if (amount == null) {
            return false;
        }
        return amount.compareTo(MIN_AMOUNT) >= 0 && amount.compareTo(MAX_AMOUNT) <= 0;
    }

    public boolean validateAccountId(String accountId) {
        if (accountId == null || accountId.isBlank()) {
            return false;
        }
        // Account IDs should be 10-16 alphanumeric characters
        return accountId.matches("^[A-Za-z0-9]{10,16}$");
    }

    public BigDecimal normalizeAmount(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
