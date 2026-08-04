package com.alumni.academic_management_api.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
@Slf4j
public class JwtService {

    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey signInKey;
    private final long expirationMs;

    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.expiration-ms}") long expirationMs
    ) {
        int secretLength = secret == null ? 0 : secret.getBytes(StandardCharsets.UTF_8).length;
        if (secretLength < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "security.jwt.secret must be at least " + MIN_SECRET_BYTES +
                            " bytes long for HS256 signing, but was " + secretLength + " bytes");
        }
        this.signInKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(String email) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(signInKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Parses and fully validates (signature + expiration) the token in a single pass.
     * Throws {@link JwtException} (e.g. ExpiredJwtException, SignatureException,
     * MalformedJwtException) if the token is invalid in any way.
     */
    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signInKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}