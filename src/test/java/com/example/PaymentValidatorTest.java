package com.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PaymentValidatorTest {

    private final PaymentValidator validator = new PaymentValidator();

    @Test
    void validateAccountId_nullOrBlank() {
        assertFalse(validator.validateAccountId(null));
        assertFalse(validator.validateAccountId(""));
        assertFalse(validator.validateAccountId("   "));
    }

    @Test
    void validateAccountId_invalidLengthsAndChars() {
        // too short
        assertFalse(validator.validateAccountId("short"));
        // too long (17 chars)
        assertFalse(validator.validateAccountId("A1B2C3D4E5F6G7H8I"));
        // invalid characters
        assertFalse(validator.validateAccountId("inv@lid1234"));
    }

    @Test
    void validateAccountId_validBoundsAndWhitespace() {
        // valid 10 chars
        assertTrue(validator.validateAccountId("A1B2C3D4E5"));
        // valid 16 chars
        assertTrue(validator.validateAccountId("A1B2C3D4E5F6G7H8"));
        // valid with surrounding whitespace
        assertTrue(validator.validateAccountId("  A1B2C3D4E5F6G7H8  "));
    }
}

