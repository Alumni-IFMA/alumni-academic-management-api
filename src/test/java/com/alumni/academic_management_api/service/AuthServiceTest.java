package com.alumni.academic_management_api.service;

import com.alumni.academic_management_api.dto.auth.LoginRequestDTO;
import com.alumni.academic_management_api.dto.auth.LoginResponseDTO;
import com.alumni.academic_management_api.entity.User;
import com.alumni.academic_management_api.exception.BusinessException;
import com.alumni.academic_management_api.repository.UserRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Nested
    class Login {
        @Test
        void givenValidCredentials_whenLogin_thenReturnToken() {
            String email = "user@email.com";
            LoginRequestDTO request = new LoginRequestDTO(email, "12345678");
            User user = User.builder()
                    .email(email)
                    .password("encrypted")
                    .build();

            Mockito.when(userRepository.findByEmail(email))
                    .thenReturn(Optional.of(user));
            Mockito.when(passwordEncoder.matches("12345678", "encrypted"))
                    .thenReturn(true);
            Mockito.when(jwtService.generateToken(email))
                    .thenReturn("jwt-token");

            LoginResponseDTO response = authService.login(request);

            assertThat(response).isNotNull();
            assertThat(response.getToken()).isEqualTo("jwt-token");
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
}
