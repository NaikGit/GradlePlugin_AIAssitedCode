package com.example;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calculates transaction fees based on payment type.
 */
public class FeeCalculator {

    private static final BigDecimal DOMESTIC_RATE = new BigDecimal("0.015");
    private static final BigDecimal INTERNATIONAL_RATE = new BigDecimal("0.035");
    private static final BigDecimal FIXED_FEE = new BigDecimal("0.30");

    public enum PaymentType {
        DOMESTIC,
        INTERNATIONAL
    }

    public BigDecimal calculateFee(BigDecimal amount, PaymentType type) {
        BigDecimal rate = type == PaymentType.INTERNATIONAL ? INTERNATIONAL_RATE : DOMESTIC_RATE;
        BigDecimal percentageFee = amount.multiply(rate);
        return percentageFee.add(FIXED_FEE).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateTotalWithFee(BigDecimal amount, PaymentType type) {
        return amount.add(calculateFee(amount, type));
    }
}
