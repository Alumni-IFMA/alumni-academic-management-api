package com.alumni.academic_management_api.service;

import com.alumni.academic_management_api.dto.auth.LoginRequestDTO;
import com.alumni.academic_management_api.dto.auth.LoginResponseDTO;
import com.alumni.academic_management_api.entity.PasswordResetToken;
import com.alumni.academic_management_api.entity.User;
import com.alumni.academic_management_api.exception.BusinessException;
import com.alumni.academic_management_api.exception.InvalidTokenException;
import com.alumni.academic_management_api.repository.PasswordResetTokenRepository;
import com.alumni.academic_management_api.repository.UserRepository;
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

    public AuthService(
            UserRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            EmailService emailService)
    {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailService = emailService;
    }

    public LoginResponseDTO login(LoginRequestDTO request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("Invalid email or password");
        }

        String token = jwtService.generateToken(user.getEmail(), user.getRole());
        return new LoginResponseDTO(token, user.getId());
    }

    @Transactional
    public void generatePasswordResetToken(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException("Email not found"));

        tokenRepository.deleteByUser(user);
        tokenRepository.flush();

        String token = UUID.randomUUID().toString();

        PasswordResetToken passwordResetToken = PasswordResetToken.builder()
                .expiryDate(LocalDateTime.now().plusHours(1))
                .user(user)
                .token(token)
                .build();

        tokenRepository.save(passwordResetToken);

        emailService.sendForgotPasswordEmail(user.getEmail(), token);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken passwordResetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid token or not found"));

        if (passwordResetToken.isExpired()){
           tokenRepository.delete(passwordResetToken);
           throw new InvalidTokenException("Token is expired");
        }

        User user = passwordResetToken.getUser();

        user.setPassword(passwordEncoder.encode(newPassword));

        userRepository.save(user);

        tokenRepository.delete(passwordResetToken);
    }

}