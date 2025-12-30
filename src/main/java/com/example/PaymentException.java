package com.example;

/**
 * Custom exception for payment processing failures.
 */
public class PaymentException extends RuntimeException {

    private final String errorCode;
    private final String transactionId;

    public PaymentException(String message, String errorCode, String transactionId) {
        super(message);
        this.errorCode = errorCode;
        this.transactionId = transactionId;
    }

    public PaymentException(String message, String errorCode, String transactionId, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.transactionId = transactionId;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getTransactionId() {
        return transactionId;
    }
}
