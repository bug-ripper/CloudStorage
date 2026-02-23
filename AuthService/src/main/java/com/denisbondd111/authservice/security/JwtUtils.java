package com.denisbondd111.authservice.security;

import com.denisbondd111.authservice.config.JwtConfig;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtUtils {

    private final JwtConfig jwtConfig;

    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtConfig.getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(UUID userId, String email, Map<String, Object> claims) {
        Instant now = Instant.now();
        Instant expiry = now.plus(jwtConfig.getAccessTokenExpiration());

        return Jwts.builder()
                .setSubject(userId.toString())
                .addClaims(claims)
                .claim("email", email)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiry))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(UUID userId) {
        Instant now = Instant.now();
        Instant expiry = now.plus(jwtConfig.getRefreshTokenExpiration());

        return Jwts.builder()
                .setSubject(userId.toString())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiry))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Jws<Claims> parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token);
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(parseToken(token).getBody().getSubject());
    }

    public boolean isExpired(String token) {
        return parseToken(token).getBody().getExpiration().before(new Date());
    }
}