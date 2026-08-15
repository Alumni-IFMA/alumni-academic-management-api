package com.alumni.academic_management_api.controller;

import com.alumni.academic_management_api.dto.auth.ForgotPasswordRequestDTO;
import com.alumni.academic_management_api.dto.auth.LoginRequestDTO;
import com.alumni.academic_management_api.dto.auth.ResetPasswordRequestDTO;
import com.alumni.academic_management_api.dto.auth.SetPasswordRequestDTO;
import com.alumni.academic_management_api.entity.PasswordResetToken;
import com.alumni.academic_management_api.entity.PasswordSetupToken;
import com.alumni.academic_management_api.entity.User;
import com.alumni.academic_management_api.enums.AccountStatus;
import com.alumni.academic_management_api.repository.PasswordResetTokenRepository;
import com.alumni.academic_management_api.repository.PasswordSetupTokenRepository;
import com.alumni.academic_management_api.repository.UserRepository;
import com.alumni.academic_management_api.service.PasswordSetupTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import io.minio.MinioClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIT {

    private static final String URL = "/auth/login";
    private static final String USER_NAME = "Joao Silva";
    private static final String USER_EMAIL = "joao@email.com";
    private static final String VALID_PASSWORD = "12345678";
    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid email or password";

    @MockBean
    private MinioClient minioClient;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private PasswordSetupTokenRepository setupTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        tokenRepository.deleteAll();
        setupTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Nested
    class Login {

        @Test
        void givenValidCredentials_whenLogin_thenReturnToken() throws Exception {
            User user = User.builder()
                    .name(USER_NAME)
                    .cpf("11111111111")
                    .email(USER_EMAIL)
                    .password(passwordEncoder.encode(VALID_PASSWORD))
                    .accountStatus(AccountStatus.ACTIVE)
                    .build();
            userRepository.save(user);

            LoginRequestDTO request = new LoginRequestDTO(USER_EMAIL, VALID_PASSWORD);

            mockMvc.perform(post(URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").isNotEmpty())
                    .andExpect(jsonPath("$.id").value(user.getId()));
        }

        @Test
        void givenInvalidPassword_whenLogin_thenReturnBadRequest() throws Exception {
            User user = User.builder()
                    .name(USER_NAME)
                    .cpf("22222222222")
                    .email(USER_EMAIL)
                    .password(passwordEncoder.encode(VALID_PASSWORD))
                    .accountStatus(AccountStatus.ACTIVE)
                    .build();
            userRepository.save(user);

            LoginRequestDTO request = new LoginRequestDTO(USER_EMAIL, "wrong-password");

            mockMvc.perform(post(URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(INVALID_CREDENTIALS_MESSAGE));
        }

        @Test
        void givenEmailNotFound_whenLogin_thenReturnBadRequest() throws Exception {
            LoginRequestDTO request = new LoginRequestDTO("notfound@email.com", VALID_PASSWORD);

            mockMvc.perform(post(URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(INVALID_CREDENTIALS_MESSAGE));
        }
    }

    @Nested
    class ForgotPassword {
        private static final String URL_FORGOT = "/auth/forgot-password";

        @Test
        void givenValidEmail_whenForgotPassword_thenReturnOk() throws Exception {
            User user = User.builder()
                    .name(USER_NAME)
                    .cpf("33333333333")
                    .email(USER_EMAIL)
                    .password(passwordEncoder.encode(VALID_PASSWORD))
                    .accountStatus(AccountStatus.ACTIVE)
                    .build();
            userRepository.save(user);

            ForgotPasswordRequestDTO request = new ForgotPasswordRequestDTO(USER_EMAIL);

            mockMvc.perform(post(URL_FORGOT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        void givenNotFoundEmail_whenForgotPassword_thenReturnBadRequest() throws Exception {
            ForgotPasswordRequestDTO request = new ForgotPasswordRequestDTO("notfound@email.com");

            mockMvc.perform(post(URL_FORGOT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("Email not found"));
        }

        @Test
        void givenInvalidEmailFormat_whenForgotPassword_thenReturnBadRequest() throws Exception {
            ForgotPasswordRequestDTO request = new ForgotPasswordRequestDTO("invalid-format");

            mockMvc.perform(post(URL_FORGOT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class ResetPassword {

        private static final String URL_RESET = "/auth/reset-password";

        @Test
        void givenValidTokenAndStrongPassword_whenResetPassword_thenReturnOk() throws Exception {
            User user = User.builder()
                    .name(USER_NAME)
                    .cpf("44444444444")
                    .email(USER_EMAIL)
                    .password(passwordEncoder.encode(VALID_PASSWORD))
                    .accountStatus(AccountStatus.ACTIVE)
                    .build();
            userRepository.save(user);

            String token = "uuid-valid-token-123";
            PasswordResetToken passwordResetToken = PasswordResetToken.builder()
                    .token(token)
                    .user(user)
                    .expiryDate(LocalDateTime.now().plusHours(1))
                    .build();
            tokenRepository.save(passwordResetToken);

            ResetPasswordRequestDTO request = new ResetPasswordRequestDTO(token, "NovaSenhaSegura123");

            mockMvc.perform(post(URL_RESET)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        void givenWeakPassword_whenResetPassword_thenReturnBadRequest() throws Exception {
            ResetPasswordRequestDTO request = new ResetPasswordRequestDTO("some-token", "123");

            mockMvc.perform(post(URL_RESET)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("A nova senha deve ter no mínimo 6 caracteres"));
        }
    }

    @Nested
    class SetPassword {

        private static final String URL_SET_PASSWORD = "/auth/set-password";
        private static final String RAW_TOKEN = "uuid-setup-token-123";
        private static final String STRONG_PASSWORD = "SenhaForte123";

        private User activeUser() {
            return User.builder()
                    .name(USER_NAME)
                    .cpf("55555555555")
                    .email(USER_EMAIL)
                    .accountStatus(AccountStatus.ACTIVE)
                    .build();
        }

        @Test
        void givenValidToken_whenSetPassword_thenReturnOk() throws Exception {
            User user = userRepository.save(activeUser());
            setupTokenRepository.save(PasswordSetupToken.builder()
                    .token(PasswordSetupTokenService.hash(RAW_TOKEN))
                    .user(user)
                    .expiresAt(LocalDateTime.now().plusHours(1))
                    .createdAt(LocalDateTime.now())
                    .build());

            SetPasswordRequestDTO request = new SetPasswordRequestDTO(RAW_TOKEN, STRONG_PASSWORD, STRONG_PASSWORD);

            mockMvc.perform(post(URL_SET_PASSWORD)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Senha definida com sucesso"));
        }

        @Test
        void givenTokenNotFound_whenSetPassword_thenReturnNotFound() throws Exception {
            SetPasswordRequestDTO request = new SetPasswordRequestDTO("unknown-token", STRONG_PASSWORD,
                    STRONG_PASSWORD);

            mockMvc.perform(post(URL_SET_PASSWORD)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        void givenAlreadyUsedToken_whenSetPassword_thenReturnGone() throws Exception {
            User user = userRepository.save(activeUser());
            setupTokenRepository.save(PasswordSetupToken.builder()
                    .token(PasswordSetupTokenService.hash(RAW_TOKEN))
                    .user(user)
                    .expiresAt(LocalDateTime.now().plusHours(1))
                    .usedAt(LocalDateTime.now().minusMinutes(5))
                    .createdAt(LocalDateTime.now())
                    .build());

            SetPasswordRequestDTO request = new SetPasswordRequestDTO(RAW_TOKEN, STRONG_PASSWORD, STRONG_PASSWORD);

            mockMvc.perform(post(URL_SET_PASSWORD)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isGone());
        }

        @Test
        void givenExpiredToken_whenSetPassword_thenReturnGone() throws Exception {
            User user = userRepository.save(activeUser());
            setupTokenRepository.save(PasswordSetupToken.builder()
                    .token(PasswordSetupTokenService.hash(RAW_TOKEN))
                    .user(user)
                    .expiresAt(LocalDateTime.now().minusMinutes(5))
                    .createdAt(LocalDateTime.now())
                    .build());

            SetPasswordRequestDTO request = new SetPasswordRequestDTO(RAW_TOKEN, STRONG_PASSWORD, STRONG_PASSWORD);

            mockMvc.perform(post(URL_SET_PASSWORD)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isGone());
        }

        @Test
        void givenPasswordsDontMatch_whenSetPassword_thenReturnBadRequest() throws Exception {
            SetPasswordRequestDTO request = new SetPasswordRequestDTO(RAW_TOKEN, STRONG_PASSWORD, "Different123");

            mockMvc.perform(post(URL_SET_PASSWORD)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }
}
