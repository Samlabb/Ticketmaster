package com.ticket.user.infrastructure;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private static final String ISSUER = "ticketmaster-user-service";
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(
            "ticketmaster-super-secret-key-for-jwt-2026-1234567890".getBytes()
    );

    public String generateAccessToken(UUID userId, String email) {
        return buildToken(userId, email, 15 * 60);
    }

    public String generateRefreshToken(UUID userId, String email) {
        return buildToken(userId, email, 7 * 24 * 60 * 60);
    }

    public String extractUserId(String token) {
        return claims(token).getSubject();
    }

    public String extractEmail(String token) {
        return claims(token).get("email", String.class);
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = claims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception ex) {
            return false;
        }
    }

    private String buildToken(UUID userId, String email, long seconds) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setIssuer(ISSUER)
                .setSubject(userId.toString())
                .claim("email", email)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(seconds)))
                .signWith(SECRET_KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    private Claims claims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .requireIssuer(ISSUER)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
