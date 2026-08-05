package com.alumni.academic_management_api.service;

import com.alumni.academic_management_api.entity.PasswordSetupToken;
import com.alumni.academic_management_api.entity.User;
import com.alumni.academic_management_api.repository.PasswordSetupTokenRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class PasswordSetupTokenService {

    private static final int EXPIRATION_HOURS = 48;

    private final PasswordSetupTokenRepository tokenRepository;

    public PasswordSetupTokenService(PasswordSetupTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    public String generateSetupToken(User user) {
        String rawToken = UUID.randomUUID().toString();

        PasswordSetupToken setupToken = PasswordSetupToken.builder()
                .user(user)
                .token(hash(rawToken))
                .expiresAt(LocalDateTime.now().plusHours(EXPIRATION_HOURS))
                .createdAt(LocalDateTime.now())
                .build();

        tokenRepository.save(setupToken);

        return rawToken;
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
