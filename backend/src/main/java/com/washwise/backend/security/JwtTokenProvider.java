package com.washwise.backend.security;

import com.washwise.backend.config.AppProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final AppProperties appProperties;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(appProperties.getSecurity().getJwtSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(String subject, String role, String userId) {
        long expirationMillis = appProperties.getSecurity().getJwtExpirationMinutes() * 60000L;
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMillis);

        return Jwts.builder()
                .subject(subject)
                .claim("role", role)
                .claim("id", userId)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }
}