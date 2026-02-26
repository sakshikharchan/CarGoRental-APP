package com.example.demo.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${app.jwt.secret:cargorent_super_secret_jwt_key_2024_must_be_at_least_256_bits_long_secure}")
    private String jwtSecret;

    @Value("${app.jwt.expiration:86400000}")
    private long jwtExpirationMs;

    @PostConstruct
    public void init() {
        logger.info("JwtUtil initialized. Secret prefix: '{}...', Expiration: {}ms",
                jwtSecret.substring(0, Math.min(10, jwtSecret.length())),
                jwtExpirationMs);
        // Validate secret length for HS256 (minimum 32 characters)
        if (jwtSecret.getBytes().length < 32) {
            throw new IllegalStateException(
                "JWT secret is too short! Must be at least 32 characters for HS256. " +
                "Current length: " + jwtSecret.length()
            );
        }
    }

    private Key getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // ── Token Generation ──────────────────────────────────────────────────────

    public String generateToken(String email, String role, Long userId) {
        // Normalize role — always store without ROLE_ prefix in token
        String normalizedRole = role.startsWith("ROLE_") ? role.substring(5) : role;

        return Jwts.builder()
                .setSubject(email)
                .claim("role", normalizedRole)
                .claim("userId", userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                // No role or userId in refresh token — intentionally minimal
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 604800000L)) // 7 days
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // ── Token Validation ──────────────────────────────────────────────────────

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (io.jsonwebtoken.security.SignatureException e) {
            logger.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            logger.error("Malformed JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("Expired JWT token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    public boolean isTokenExpired(String token) {
        try {
            return getClaims(token).getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    // ── Claims Extraction ─────────────────────────────────────────────────────

    public String getEmailFromToken(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * Returns role WITHOUT "ROLE_" prefix (e.g. "VENDOR", "ADMIN", "CUSTOMER").
     * JwtAuthFilter adds "ROLE_" prefix before creating the authority.
     */
    public String getRoleFromToken(String token) {
        return (String) getClaims(token).get("role");
    }

    public Long getUserIdFromToken(String token) {
        Object userId = getClaims(token).get("userId");
        if (userId == null) return null;
        return Long.valueOf(userId.toString());
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}