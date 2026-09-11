package com.ticket.user;

import com.ticket.user.infrastructure.JwtService;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JwtServiceTest {

    @Test
    void shouldGenerateAndValidateAccessAndRefreshTokens() {
        JwtService jwtService = new JwtService();
        UUID userId = UUID.randomUUID();

        String accessToken = jwtService.generateAccessToken(userId, "test@example.com");
        String refreshToken = jwtService.generateRefreshToken(userId, "test@example.com");

        assertNotNull(accessToken);
        assertNotNull(refreshToken);
        assertEquals(userId.toString(), jwtService.extractUserId(accessToken));
        assertEquals(userId.toString(), jwtService.extractUserId(refreshToken));
        assertEquals("test@example.com", jwtService.extractEmail(accessToken));
        assertEquals("test@example.com", jwtService.extractEmail(refreshToken));
    }
}
