package com.example.demo.security;

import com.example.demo.model.Role;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expirationMs;

    public JwtUtil(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs
    ) {
        this.key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
        this.expirationMs = expirationMs;
    }

    public String generateToken(UUID memberId, UUID churchId, String email, Role role, int tokenVersion) {
        JwtBuilder builder = Jwts.builder()
                .claim("memberId", memberId.toString())
                .claim("churchId", churchId.toString())
                .claim("role", role.name())
                .claim("tokenVersion", tokenVersion)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key);
        if (email != null && !email.isBlank()) {
            builder = builder.subject(email);
        }
        return builder.compact();
    }

    public int extractTokenVersion(String token) {
        Object v = parseClaims(token).get("tokenVersion");
        if (v == null) return 0;
        return ((Number) v).intValue();
    }

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public UUID extractMemberId(String token) {
        return UUID.fromString(parseClaims(token).get("memberId", String.class));
    }

    public UUID extractChurchId(String token) {
        return UUID.fromString(parseClaims(token).get("churchId", String.class));
    }

    public Role extractRole(String token) {
        return Role.valueOf(parseClaims(token).get("role", String.class));
    }

    public String generateAttendanceToken(UUID churchId, String serviceName, LocalDate serviceDate) {
        return Jwts.builder()
                .subject("attendance-qr")
                .claim("churchId", churchId.toString())
                .claim("serviceName", serviceName)
                .claim("serviceDate", serviceDate.toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 8 * 60 * 60 * 1000L))
                .signWith(key)
                .compact();
    }

    public UUID extractAttendanceChurchId(String token) {
        return UUID.fromString(parseClaims(token).get("churchId", String.class));
    }

    public String extractAttendanceServiceName(String token) {
        return parseClaims(token).get("serviceName", String.class);
    }

    public LocalDate extractAttendanceServiceDate(String token) {
        return LocalDate.parse(parseClaims(token).get("serviceDate", String.class));
    }

    public boolean isAttendanceToken(String token) {
        try {
            return "attendance-qr".equals(parseClaims(token).getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public LocalDateTime extractIssuedAt(String token) {
        Instant instant = parseClaims(token).getIssuedAt().toInstant();
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
