package com.example;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Pattern;

/**
 * Validates payment requests.
 */
public class PaymentValidator {

    private static final BigDecimal MIN_AMOUNT = new BigDecimal("0.01");
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000.00");
    // Precompiled pattern for account ID validation (10-16 ASCII alphanumeric chars)
    private static final Pattern ACCOUNT_ID_PATTERN = Pattern.compile("^[A-Za-z0-9]{10,16}$");

    public boolean validateAmount(BigDecimal amount) {
        if (amount == null) {
            return false;
        }
        return amount.compareTo(MIN_AMOUNT) >= 0 && amount.compareTo(MAX_AMOUNT) <= 0;
    }

    public boolean validateAccountId(String accountId) {
        // Null check
        if (accountId == null) {
            return false;
        }
        // Trim surrounding whitespace and perform a quick length check before regex
        String trimmed = accountId.trim();
        if (trimmed.length() < 10 || trimmed.length() > 16) {
            return false;
        }
        // Account IDs should be 10-16 ASCII alphanumeric characters
        return ACCOUNT_ID_PATTERN.matcher(trimmed).matches();
    }

    public BigDecimal normalizeAmount(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
