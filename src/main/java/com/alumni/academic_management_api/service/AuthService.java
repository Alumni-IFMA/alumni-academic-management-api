package com.alumni.academic_management_api.service;

import com.alumni.academic_management_api.dto.auth.LoginRequestDTO;
import com.alumni.academic_management_api.dto.auth.LoginResponseDTO;
import com.alumni.academic_management_api.entity.PasswordResetToken;
import com.alumni.academic_management_api.entity.User;
import com.alumni.academic_management_api.enums.AccountStatus;
import com.alumni.academic_management_api.exception.BusinessException;
import com.alumni.academic_management_api.exception.InvalidTokenException;
import com.alumni.academic_management_api.repository.PasswordResetTokenRepository;
import com.alumni.academic_management_api.repository.UserRepository;
import com.alumni.academic_management_api.util.TokenHasher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
            UserRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            EmailService emailService,
            RefreshTokenService refreshTokenService)
    {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailService = emailService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public LoginResponseDTO login(LoginRequestDTO request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("Invalid email or password");
        }

        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            String message = switch (user.getAccountStatus()) {
                case PENDING_VERIFICATION -> "Account is pending approval";
                case SUSPENDED -> "Account is suspended";
                default -> "Account is not active";
            };
            throw new BusinessException(message);
        }

        String accessToken = jwtService.generateToken(user.getEmail());
        String refreshToken = refreshTokenService.generate(user);
        return new LoginResponseDTO(accessToken, refreshToken, user.getId());
    }

    @Transactional
    public LoginResponseDTO refreshToken(String rawRefreshToken) {
        RefreshTokenService.RotationResult rotation = refreshTokenService.rotate(rawRefreshToken);
        User user = rotation.user();
        String accessToken = jwtService.generateToken(user.getEmail());
        return new LoginResponseDTO(accessToken, rotation.rawToken(), user.getId());
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }

    @Transactional
    public void generatePasswordResetToken(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Email not found"));

        tokenRepository.deleteByUser(user);
        tokenRepository.flush();

        String rawToken = UUID.randomUUID().toString();

        PasswordResetToken passwordResetToken = PasswordResetToken.builder()
                .expiryDate(LocalDateTime.now().plusHours(1))
                .user(user)
                .token(TokenHasher.sha256(rawToken))
                .build();

        tokenRepository.save(passwordResetToken);

        emailService.sendForgotPasswordEmail(user.getEmail(), rawToken);
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        PasswordResetToken passwordResetToken = tokenRepository.findByToken(TokenHasher.sha256(rawToken))
                .orElseThrow(() -> new InvalidTokenException("Invalid token or not found"));

        if (passwordResetToken.isExpired()){
           tokenRepository.delete(passwordResetToken);
           throw new InvalidTokenException("Token is expired");
        }

        User user = passwordResetToken.getUser();

        user.setPassword(passwordEncoder.encode(newPassword));

        userRepository.save(user);

        tokenRepository.delete(passwordResetToken);

        refreshTokenService.revokeAllForUser(user);
    }

}