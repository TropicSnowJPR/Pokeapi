package com.pokeapi.papi;

import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class RandomBytesTest {

    @Test
    void testUrlSafeBase64() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String value = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        System.out.println("Generated URL-safe Base64 value: " + value);

        // Basic assertions about the generated value
        assertNotNull(value, "generated value should not be null");
        // For 32 bytes, Base64 URL-safe without padding produces 43 characters
        assertEquals(43, value.length(), "unexpected encoded length for 32 bytes without padding");
        // Ensure it's URL-safe base64 characters only (no padding '=' present)
        assertTrue(value.matches("^[A-Za-z0-9_-]+$"), "value contains only URL-safe base64 characters");
    }
}

