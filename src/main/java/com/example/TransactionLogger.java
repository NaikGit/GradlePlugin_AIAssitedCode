package com.example;

import java.time.Instant;
import java.util.logging.Logger;

/**
 * Logs payment transactions for audit purposes.
 */
public class TransactionLogger {

    private static final Logger LOGGER = Logger.getLogger(TransactionLogger.class.getName());

    public void logTransaction(String transactionId, String accountId, String amount, String status) {
        String message = String.format(
            "[%s] Transaction %s: Account=%s, Amount=%s, Status=%s",
            Instant.now(),
            transactionId,
            maskAccountId(accountId),
            amount,
            status
        );
        LOGGER.info(message);
    }

    private String maskAccountId(String accountId) {
        if (accountId == null || accountId.length() < 4) {
            return "****";
        }
        return "****" + accountId.substring(accountId.length() - 4);
    }
}
