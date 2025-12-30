package com.example;

/**
 * Sample class for demonstrating the AI Attribution plugin.
 */
public class PaymentService {

    public boolean processPayment(String accountId, double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        
        // Simulate payment processing
        System.out.println("Processing payment of $" + amount + " for account " + accountId);
        return true;
    }
    
    public double calculateFee(double amount) {
        // Simple fee calculation
        return amount * 0.029 + 0.30;
    }
}
