package com.ticket.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

public class JwtValidator {

    protected static final String ISSUER = "ticketmaster-user-service";

    public static final String CLAIM_TYPE = "typ";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

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

    /** Подпись + срок действия, тип токена не проверяется. Для бизнес-логики лучше использовать методы ниже. */
    public boolean isTokenValid(String token) {
        try {
            Claims claims = claims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean isAccessTokenValid(String token) {
        return hasValidType(token, TYPE_ACCESS);
    }

    public boolean isRefreshTokenValid(String token) {
        return hasValidType(token, TYPE_REFRESH);
    }

    /** Возвращает пользователя, только если токен валиден и это именно access-токен. */
    public Optional<TokenPrincipal> parseAccessToken(String token) {
        try {
            Claims claims = claims(token);
            if (!TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class))) {
                return Optional.empty();
            }
            if (claims.getExpiration() == null || !claims.getExpiration().after(new Date())) {
                return Optional.empty();
            }
            String roleValue = claims.get("role", String.class);
            Role role = roleValue == null ? Role.USER : Role.valueOf(roleValue);
            return Optional.of(new TokenPrincipal(claims.getSubject(), claims.get("email", String.class), role));
        } catch (Exception ex) {
            return Optional.empty();
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

    private boolean hasValidType(String token, String expectedType) {
        try {
            Claims claims = claims(token);
            return claims.getExpiration().after(new Date())
                    && expectedType.equals(claims.get(CLAIM_TYPE, String.class));
        } catch (Exception ex) {
            return false;
        }
    }
}