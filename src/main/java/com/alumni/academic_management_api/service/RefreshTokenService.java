package com.alumni.academic_management_api.service;

import com.alumni.academic_management_api.entity.RefreshToken;
import com.alumni.academic_management_api.entity.User;
import com.alumni.academic_management_api.exception.InvalidTokenException;
import com.alumni.academic_management_api.repository.RefreshTokenRepository;
import com.alumni.academic_management_api.util.TokenHasher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenRepository refreshTokenRepository;
    private final long refreshExpirationMs;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${security.jwt.refresh-expiration-ms}") long refreshExpirationMs
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    @Transactional
    public String generate(User user) {
        String rawToken = randomToken();

        RefreshToken refreshToken = RefreshToken.builder()
                .tokenHash(TokenHasher.sha256(rawToken))
                .user(user)
                .expiryDate(LocalDateTime.now().plus(Duration.ofMillis(refreshExpirationMs)))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    @Transactional
    public RotationResult rotate(String rawToken) {
        RefreshToken stored = refreshTokenRepository.findByTokenHash(TokenHasher.sha256(rawToken))
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        if (!stored.isValid()) {
            throw new InvalidTokenException("Refresh token is expired or has been revoked");
        }

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        User user = stored.getUser();
        String newRawToken = generate(user);
        return new RotationResult(user, newRawToken);
    }

    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(TokenHasher.sha256(rawToken))
                .ifPresent(stored -> {
                    stored.setRevoked(true);
                    refreshTokenRepository.save(stored);
                });
    }

    @Transactional
    public void revokeAllForUser(User user) {
        refreshTokenRepository.revokeAllByUser(user);
    }

    private String randomToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    public record RotationResult(User user, String rawToken) {
    }
}