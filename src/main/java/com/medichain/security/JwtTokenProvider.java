package com.medichain.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

@Component
@Slf4j
public class JwtTokenProvider {

    private final SecretKey accessTokenSecret;
    private final SecretKey refreshTokenSecret;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtTokenProvider(
            @Value("${medichain.security.jwt.secret}") String secret,
            @Value("${medichain.security.jwt.refresh-secret:}") String refreshSecret,
            @Value("${medichain.security.jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${medichain.security.jwt.refresh-token-expiration}") long refreshTokenExpiration) {
        var accessKeyBytes = secret.getBytes(StandardCharsets.UTF_8);
        this.accessTokenSecret = Keys.hmacShaKeyFor(accessKeyBytes);
        var refreshKeyMaterial = refreshSecret.isBlank() ? (secret + "_refresh") : refreshSecret;
        this.refreshTokenSecret = Keys.hmacShaKeyFor(refreshKeyMaterial.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public String generateAccessToken(UUID userId, String username, String role,
                                       UUID hospitalId, UUID wardId, Set<String> permissions) {
        var now = new Date();
        return Jwts.builder()
            .subject(userId.toString())
            .claim("username", username)
            .claim("role", role)
            .claim("hospitalId", hospitalId != null ? hospitalId.toString() : null)
            .claim("wardId", wardId != null ? wardId.toString() : null)
            .claim("permissions", permissions)
            .issuedAt(now)
            .expiration(new Date(now.getTime() + accessTokenExpiration))
            .signWith(accessTokenSecret)
            .compact();
    }

    public String generateRefreshToken(UUID userId) {
        var now = new Date();
        return Jwts.builder()
            .subject(userId.toString())
            .claim("type", "refresh")
            .issuedAt(now)
            .expiration(new Date(now.getTime() + refreshTokenExpiration))
            .signWith(refreshTokenSecret)
            .compact();
    }

    public Claims parseAccessToken(String token) {
        return Jwts.parser()
            .verifyWith(accessTokenSecret)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            parseAccessToken(token);
            return true;
        } catch (SecurityException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims empty: {}", e.getMessage());
        }
        return false;
    }

    public UUID getUserIdFromToken(String token) {
        return UUID.fromString(parseAccessToken(token).getSubject());
    }

    public String getRoleFromToken(String token) {
        return parseAccessToken(token).get("role", String.class);
    }

    public String getWardIdFromToken(String token) {
        return parseAccessToken(token).get("wardId", String.class);
    }
}
