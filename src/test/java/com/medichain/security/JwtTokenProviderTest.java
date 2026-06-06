package com.medichain.security;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenProvider = new JwtTokenProvider(
            "test_secret_key_that_is_at_least_512_bits_long_for_hmac_algorithm_requirements",
            "test_refresh_secret_for_hmac_algorithm_separation",
            3600000L, 604800000L);
    }

    @Test
    void generateAccessToken_shouldProduceValidToken() {
        var userId = UUID.randomUUID();
        var token = tokenProvider.generateAccessToken(
            userId, "pharmacist1", "WARD_PHARMACIST",
            UUID.randomUUID(), UUID.randomUUID(), Set.of("VIEW_OWN_WARD", "DISPENSE"));

        assertNotNull(token);
        assertTrue(token.startsWith("eyJ"));
        assertTrue(tokenProvider.validateToken(token));
    }

    @Test
    void validateToken_shouldRejectExpiredToken() {
        var shortTokenProvider = new JwtTokenProvider(
            "test_secret_key_that_is_at_least_512_bits_long_for_hmac_algorithm_requirements",
            "test_refresh_secret_for_hmac_algorithm_separation",
            -3600000L, 604800000L);

        var token = shortTokenProvider.generateAccessToken(
            UUID.randomUUID(), "user", "ADMIN", null, null, Set.of("ALL"));

        assertFalse(tokenProvider.validateToken(token));
    }

    @Test
    void validateToken_shouldRejectInvalidToken() {
        assertFalse(tokenProvider.validateToken("invalid.jwt.token"));
    }

    @Test
    void getUserIdFromToken_shouldReturnCorrectId() {
        var userId = UUID.randomUUID();
        var token = tokenProvider.generateAccessToken(
            userId, "manager1", "PHARMACY_MANAGER",
            UUID.randomUUID(), null, Set.of("VIEW_ALL_WARDS", "MANAGE_PROCUREMENT"));

        var extractedId = tokenProvider.getUserIdFromToken(token);
        assertEquals(userId, extractedId);
    }

    @Test
    void getRoleFromToken_shouldReturnCorrectRole() {
        var token = tokenProvider.generateAccessToken(
            UUID.randomUUID(), "admin1", "ADMIN",
            UUID.randomUUID(), null, Set.of("ALL"));

        assertEquals("ADMIN", tokenProvider.getRoleFromToken(token));
    }

    @Test
    void generateRefreshToken_shouldProduceValidToken() {
        var userId = UUID.randomUUID();
        var refreshToken = tokenProvider.generateRefreshToken(userId);

        assertNotNull(refreshToken);
        assertTrue(tokenProvider.validateToken(refreshToken));
    }
}
