package com.ticket.user.infrastructure;

import com.ticket.security.JwtValidator;
import com.ticket.security.Role;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService extends JwtValidator {

    public JwtService(@Value("${app.jwt.secret}") String secret) {
        super(secret);
    }

    public String generateAccessToken(UUID userId, String email, Role role) {
        return buildToken(userId, email, role, 15 * 60, TYPE_ACCESS);
    }

    public String generateRefreshToken(UUID userId, String email, Role role) {
        return buildToken(userId, email, role, 7 * 24 * 60 * 60, TYPE_REFRESH);
    }

    private String buildToken(UUID userId, String email, Role role, long seconds, String type) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setIssuer(ISSUER)
                .setSubject(userId.toString())
                .claim("email", email)
                .claim("role", role.name())
                .claim(CLAIM_TYPE, type)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(seconds)))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }
}