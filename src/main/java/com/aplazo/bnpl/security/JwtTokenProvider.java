package com.aplazo.bnpl.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);
    private static final String ROLE_CLAIM = "role";
    private static final String DEFAULT_ROLE = "ROLE_CUSTOMER";

    private final String secret;
    private final long expirationMs;
    private final SecretKey signingKey;

    public JwtTokenProvider(
            @Value("${security.jwt.secret:${app.jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}}") String secret,
            @Value("${security.jwt.expiration-ms:${app.jwt.expiration-ms:86400000}}") long expirationMs
    ) {
        this.secret = secret;
        this.expirationMs = expirationMs;
        this.signingKey = initSigningKey(secret);
    }

    private SecretKey initSigningKey(String secretKey) {
        byte[] keyBytes;
        try {
            byte[] decoded = Decoders.BASE64.decode(secretKey);
            if (decoded.length >= 32) {
                keyBytes = decoded;
            } else {
                keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(UUID customerId) {
        return generateToken(customerId, DEFAULT_ROLE);
    }

    public String generateToken(UUID customerId, String role) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(expirationMs);

        return Jwts.builder()
                .subject(customerId.toString())
                .claim(ROLE_CLAIM, role != null ? role : DEFAULT_ROLE)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token.trim());
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    public UUID getCustomerIdFromToken(String token) {
        Claims claims = getClaims(token);
        return UUID.fromString(claims.getSubject());
    }

    public Authentication getAuthentication(String token) {
        Claims claims = getClaims(token);
        UUID customerId = UUID.fromString(claims.getSubject());
        String role = claims.get(ROLE_CLAIM, String.class);
        if (role == null || role.isBlank()) {
            role = DEFAULT_ROLE;
        }
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));
        return new UsernamePasswordAuthenticationToken(customerId, token, authorities);
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token.trim())
                .getPayload();
    }
}
