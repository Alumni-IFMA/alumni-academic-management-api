package com.alumni.academic_management_api.service;

import com.alumni.academic_management_api.dto.auth.LoginRequestDTO;
import com.alumni.academic_management_api.dto.auth.LoginResponseDTO;
import com.alumni.academic_management_api.entity.PasswordResetToken;
import com.alumni.academic_management_api.entity.PasswordSetupToken;
import com.alumni.academic_management_api.entity.User;
import com.alumni.academic_management_api.enums.AccountStatus;
import com.alumni.academic_management_api.exception.BusinessException;
import com.alumni.academic_management_api.exception.InvalidTokenException;
import com.alumni.academic_management_api.exception.ResourceNotFoundException;
import com.alumni.academic_management_api.exception.TokenGoneException;
import com.alumni.academic_management_api.repository.PasswordResetTokenRepository;
import com.alumni.academic_management_api.repository.PasswordSetupTokenRepository;
import com.alumni.academic_management_api.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class AuthService {

    private static final Pattern PASSWORD_STRENGTH_PATTERN =
            Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,}$");

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final PasswordSetupTokenRepository passwordSetupTokenRepository;

    public AuthService(
            UserRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            EmailService emailService,
            PasswordSetupTokenRepository passwordSetupTokenRepository)
    {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailService = emailService;
        this.passwordSetupTokenRepository = passwordSetupTokenRepository;
    }

    public LoginResponseDTO login(LoginRequestDTO request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("Invalid email or password");
        }

        String token = jwtService.generateToken(user.getEmail(), user.getRole());
        return new LoginResponseDTO(token, user.getId(), user.getHasSeenTutorial());
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

    @Transactional
    public void setPassword(String token, String password, String passwordConfirmation) {
        if (!password.equals(passwordConfirmation)) {
            throw new BusinessException("Passwords do not match");
        }

        if (!PASSWORD_STRENGTH_PATTERN.matcher(password).matches()) {
            throw new BusinessException("Password must be at least 8 characters long and contain "
                    + "at least one letter and one number");
        }

        String tokenHash = PasswordSetupTokenService.hash(token);
        PasswordSetupToken setupToken = passwordSetupTokenRepository.findByToken(tokenHash)
                .orElseThrow(() -> new ResourceNotFoundException("Token not found"));

        if (setupToken.getUsedAt() != null) {
            throw new TokenGoneException("Token has already been used");
        }

        if (setupToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new TokenGoneException("Token is expired");
        }

        User user = setupToken.getUser();

        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException("User is not active");
        }

        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);

        setupToken.setUsedAt(LocalDateTime.now());
        passwordSetupTokenRepository.save(setupToken);
    }

}