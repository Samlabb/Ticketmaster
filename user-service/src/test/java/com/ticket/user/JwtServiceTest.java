package com.ticket.user;

import com.ticket.security.Role;
import com.ticket.user.infrastructure.JwtService;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JwtServiceTest {

    private static final String TEST_SECRET = "test-secret-key-for-jwt-tests-minimum-32-bytes-long";

    @Test
    void shouldGenerateAndValidateAccessAndRefreshTokens() {
        JwtService jwtService = new JwtService(TEST_SECRET);
        UUID userId = UUID.randomUUID();

        String accessToken = jwtService.generateAccessToken(userId, "test@example.com", Role.USER);
        String refreshToken = jwtService.generateRefreshToken(userId, "test@example.com", Role.USER);

        assertNotNull(accessToken);
        assertNotNull(refreshToken);
        assertEquals(userId.toString(), jwtService.extractUserId(accessToken));
        assertEquals(userId.toString(), jwtService.extractUserId(refreshToken));
        assertEquals("test@example.com", jwtService.extractEmail(accessToken));
        assertEquals("test@example.com", jwtService.extractEmail(refreshToken));
        assertEquals(Role.USER, jwtService.extractRole(accessToken));
        assertEquals(Role.USER, jwtService.extractRole(refreshToken));
    }

    @Test
    void shouldExtractAdminRoleFromToken() {
        JwtService jwtService = new JwtService(TEST_SECRET);
        UUID userId = UUID.randomUUID();

        String accessToken = jwtService.generateAccessToken(userId, "admin@example.com", Role.ADMIN);

        assertEquals(Role.ADMIN, jwtService.extractRole(accessToken));
    }
}