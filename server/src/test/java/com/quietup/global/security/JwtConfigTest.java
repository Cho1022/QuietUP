package com.quietup.global.security;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;

class JwtConfigTest {

    private final JwtConfig jwtConfig = new JwtConfig();

    @Test
    void rejectsInvalidBase64Secret() {
        JwtProperties properties = new JwtProperties("not-valid-base64!", 900, 14);

        assertThrows(IllegalStateException.class, () -> jwtConfig.jwtSecretKey(properties));
    }

    @Test
    void rejectsSecretShorterThanHs256Minimum() {
        String shortSecret = Base64.getEncoder()
                .encodeToString("short-test-key".getBytes(StandardCharsets.UTF_8));
        JwtProperties properties = new JwtProperties(shortSecret, 900, 14);

        assertThrows(IllegalStateException.class, () -> jwtConfig.jwtSecretKey(properties));
    }
}
