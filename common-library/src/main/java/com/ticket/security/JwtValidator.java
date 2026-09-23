package com.ticket.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JwtValidator {

    protected static final String ISSUER = "ticketmaster-user-service";
    protected final SecretKey secretKey;

    public JwtValidator(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret не задан. Установите переменную окружения JWT_SECRET перед запуском.");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "JWT secret слишком короткий.");
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = claims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception ex) {
            return false;
        }
    }

    public String extractUserId(String token) {
        return claims(token).getSubject();
    }

    public String extractEmail(String token) {
        return claims(token).get("email", String.class);
    }

    public Role extractRole(String token) {
        String roleValue = claims(token).get("role", String.class);
        return roleValue == null ? Role.USER : Role.valueOf(roleValue);
    }

    protected Claims claims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .requireIssuer(ISSUER)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}