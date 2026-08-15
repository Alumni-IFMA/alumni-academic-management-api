package com.alumni.academic_management_api.service;

import com.alumni.academic_management_api.dto.auth.LoginRequestDTO;
import com.alumni.academic_management_api.dto.auth.LoginResponseDTO;
import com.alumni.academic_management_api.entity.PasswordResetToken;
import com.alumni.academic_management_api.entity.PasswordSetupToken;
import com.alumni.academic_management_api.entity.User;
import com.alumni.academic_management_api.enums.AccountStatus;
import com.alumni.academic_management_api.enums.Role;
import com.alumni.academic_management_api.exception.BusinessException;
import com.alumni.academic_management_api.exception.InvalidTokenException;
import com.alumni.academic_management_api.exception.ResourceNotFoundException;
import com.alumni.academic_management_api.exception.TokenGoneException;
import com.alumni.academic_management_api.repository.PasswordResetTokenRepository;
import com.alumni.academic_management_api.repository.PasswordSetupTokenRepository;
import com.alumni.academic_management_api.repository.UserRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordSetupTokenRepository passwordSetupTokenRepository;

    @InjectMocks
    private AuthService authService;

    @Nested
    class Login {
        @Test
        void givenValidCredentials_whenLogin_thenReturnToken() {
            String email = "user@email.com";
            LoginRequestDTO request = new LoginRequestDTO(email, "12345678");
            User user = User.builder()
                    .id(42L)
                    .email(email)
                    .password("encrypted")
                    .build();

            Mockito.when(userRepository.findByEmail(email))
                    .thenReturn(Optional.of(user));
            Mockito.when(passwordEncoder.matches("12345678", "encrypted"))
                    .thenReturn(true);
            Mockito.when(jwtService.generateToken(email, Role.ALUMNI))
                    .thenReturn("jwt-token");

            LoginResponseDTO response = authService.login(request);

            assertThat(response).isNotNull();
            assertThat(response.getToken()).isEqualTo("jwt-token");
            assertThat(response.getId()).isEqualTo(42L);
        }

        @Test
        void givenInvalidEmail_whenLogin_thenThrowBusinessException() {
            LoginRequestDTO request = new LoginRequestDTO("invalid@email.com", "12345678");

            Mockito.when(userRepository.findByEmail("invalid@email.com"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Invalid email or password");
        }

        @Test
        void givenInvalidPassword_whenLogin_thenThrowBusinessException() {
            String email = "user@email.com";
            LoginRequestDTO request = new LoginRequestDTO(email, "wrong-pass");
            User user = User.builder()
                    .email(email)
                    .password("encrypted")
                    .build();

            Mockito.when(userRepository.findByEmail(email))
                    .thenReturn(Optional.of(user));
            Mockito.when(passwordEncoder.matches("wrong-pass", "encrypted"))
                    .thenReturn(false);

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Invalid email or password");
        }
    }

    @Nested
    class GeneratePasswordResetToken {
        @Test
        void givenValidEmail_whenGenerateToken_thenSaveTokenAndSendEmail() {
            String email = "user@email.com";

            User user = User.builder().email(email).build();

            Mockito.when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

            authService.generatePasswordResetToken(email);

            Mockito.verify(passwordResetTokenRepository).deleteByUser(user);
            Mockito.verify(passwordResetTokenRepository).flush();
            Mockito.verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
            Mockito.verify(emailService).sendForgotPasswordEmail(Mockito.eq(email), Mockito.anyString());
        }

        @Test
        void givenInvalidEmail_whenGenerateToken_thenThrowBusinessException() {
            String email = "invalid@email.com";

            Mockito.when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.generatePasswordResetToken(email))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Email not found");

            Mockito.verify(passwordResetTokenRepository, Mockito.never()).save(any());
            Mockito.verify(emailService, Mockito.never()).sendForgotPasswordEmail(any(), any());
        }
    }

    @Nested
    class ResetPassword {

        @Test
        void givenValidTokenAndNewPassword_whenResetPassword_thenUpdatePasswordAndDeleteToken() {
            String token = "valid-token";
            String newPassword = "new-secure-password";
            User user = User.builder().email("user@email.com").build();

            PasswordResetToken passwordResetToken = PasswordResetToken.builder()
                    .token(token)
                    .user(user)
                    .expiryDate(LocalDateTime.now().plusHours(1))
                    .build();

            Mockito.when(passwordResetTokenRepository.findByToken(token)).thenReturn(Optional.of(passwordResetToken));
            Mockito.when(passwordEncoder.encode(newPassword)).thenReturn("encoded-new-password");

            authService.resetPassword(token, newPassword);

            assertThat(user.getPassword()).isEqualTo("encoded-new-password");
            Mockito.verify(userRepository).save(user);
            Mockito.verify(passwordResetTokenRepository).delete(passwordResetToken);
        }

        @Test
        void givenInvalidToken_whenResetPassword_thenThrowInvalidTokenException() {
            String token = "invalid-token";

            Mockito.when(passwordResetTokenRepository.findByToken(token)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.resetPassword(token, "password123"))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("Invalid token or not found");

            Mockito.verify(userRepository, Mockito.never()).save(any());
        }

        @Test
        void givenExpiredToken_whenResetPassword_thenDeleteTokenAndThrowException() {
            String token = "expired-token";
            User user = User.builder().email("user@email.com").build();

            PasswordResetToken passwordResetToken = PasswordResetToken.builder()
                    .token(token)
                    .user(user)
                    .expiryDate(LocalDateTime.now().minusMinutes(10))
                    .build();

            Mockito.when(passwordResetTokenRepository.findByToken(token)).thenReturn(Optional.of(passwordResetToken));

            assertThatThrownBy(() -> authService.resetPassword(token, "password123"))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("Token is expired");

            Mockito.verify(passwordResetTokenRepository).delete(passwordResetToken);
            Mockito.verify(userRepository, Mockito.never()).save(any());
        }
    }

    @Nested
    class SetPassword {

        private static final String RAW_TOKEN = "raw-setup-token";

        @Test
        void shouldSetPasswordSuccessfully() {
            User user = User.builder().id(7L).email("user@email.com").accountStatus(AccountStatus.ACTIVE).build();
            PasswordSetupToken setupToken = PasswordSetupToken.builder()
                    .token(PasswordSetupTokenService.hash(RAW_TOKEN))
                    .user(user)
                    .expiresAt(LocalDateTime.now().plusHours(1))
                    .build();

            Mockito.when(passwordSetupTokenRepository.findByToken(PasswordSetupTokenService.hash(RAW_TOKEN)))
                    .thenReturn(Optional.of(setupToken));
            Mockito.when(passwordEncoder.encode("Password1")).thenReturn("encoded-password");

            authService.setPassword(RAW_TOKEN, "Password1", "Password1");

            assertThat(user.getPassword()).isEqualTo("encoded-password");
            assertThat(setupToken.getUsedAt()).isNotNull();
            Mockito.verify(userRepository).save(user);
            Mockito.verify(passwordSetupTokenRepository).save(setupToken);
        }

        @Test
        void shouldThrow_whenPasswordsDontMatch() {
            assertThatThrownBy(() -> authService.setPassword(RAW_TOKEN, "Password1", "Password2"))
                    .isInstanceOf(BusinessException.class);

            Mockito.verify(passwordSetupTokenRepository, Mockito.never()).findByToken(any());
        }

        @Test
        void shouldThrow_whenPasswordTooWeak() {
            assertThatThrownBy(() -> authService.setPassword(RAW_TOKEN, "weak", "weak"))
                    .isInstanceOf(BusinessException.class);

            Mockito.verify(passwordSetupTokenRepository, Mockito.never()).findByToken(any());
        }

        @Test
        void shouldThrow_whenTokenNotFound() {
            Mockito.when(passwordSetupTokenRepository.findByToken(PasswordSetupTokenService.hash(RAW_TOKEN)))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.setPassword(RAW_TOKEN, "Password1", "Password1"))
                    .isInstanceOf(ResourceNotFoundException.class);

            Mockito.verify(userRepository, Mockito.never()).save(any());
        }

        @Test
        void shouldThrow_whenTokenAlreadyUsed() {
            User user = User.builder().id(7L).email("user@email.com").accountStatus(AccountStatus.ACTIVE).build();
            PasswordSetupToken setupToken = PasswordSetupToken.builder()
                    .token(PasswordSetupTokenService.hash(RAW_TOKEN))
                    .user(user)
                    .expiresAt(LocalDateTime.now().plusHours(1))
                    .usedAt(LocalDateTime.now().minusMinutes(5))
                    .build();

            Mockito.when(passwordSetupTokenRepository.findByToken(PasswordSetupTokenService.hash(RAW_TOKEN)))
                    .thenReturn(Optional.of(setupToken));

            assertThatThrownBy(() -> authService.setPassword(RAW_TOKEN, "Password1", "Password1"))
                    .isInstanceOf(TokenGoneException.class);

            Mockito.verify(userRepository, Mockito.never()).save(any());
        }

        @Test
        void shouldThrow_whenTokenExpired() {
            User user = User.builder().id(7L).email("user@email.com").accountStatus(AccountStatus.ACTIVE).build();
            PasswordSetupToken setupToken = PasswordSetupToken.builder()
                    .token(PasswordSetupTokenService.hash(RAW_TOKEN))
                    .user(user)
                    .expiresAt(LocalDateTime.now().minusMinutes(5))
                    .build();

            Mockito.when(passwordSetupTokenRepository.findByToken(PasswordSetupTokenService.hash(RAW_TOKEN)))
                    .thenReturn(Optional.of(setupToken));

            assertThatThrownBy(() -> authService.setPassword(RAW_TOKEN, "Password1", "Password1"))
                    .isInstanceOf(TokenGoneException.class);

            Mockito.verify(userRepository, Mockito.never()).save(any());
        }

        @Test
        void shouldThrow_whenUserNotActive() {
            User user = User.builder().id(7L).email("user@email.com")
                    .accountStatus(AccountStatus.PENDING_VERIFICATION).build();
            PasswordSetupToken setupToken = PasswordSetupToken.builder()
                    .token(PasswordSetupTokenService.hash(RAW_TOKEN))
                    .user(user)
                    .expiresAt(LocalDateTime.now().plusHours(1))
                    .build();

            Mockito.when(passwordSetupTokenRepository.findByToken(PasswordSetupTokenService.hash(RAW_TOKEN)))
                    .thenReturn(Optional.of(setupToken));

            assertThatThrownBy(() -> authService.setPassword(RAW_TOKEN, "Password1", "Password1"))
                    .isInstanceOf(BusinessException.class);

            Mockito.verify(userRepository, Mockito.never()).save(any());
        }
    }
}
