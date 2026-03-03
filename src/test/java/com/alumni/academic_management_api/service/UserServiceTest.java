package com.alumni.academic_management_api.service;

import com.alumni.academic_management_api.dto.user.RegisterRequestDTO;
import com.alumni.academic_management_api.dto.user.UserSimpleDTO;
import com.alumni.academic_management_api.entity.User;
import com.alumni.academic_management_api.exception.BusinessException;
import com.alumni.academic_management_api.mapper.UserMapper;
import com.alumni.academic_management_api.repository.UserRepository;
import com.alumni.academic_management_api.service.validation.UserValidator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserValidator userValidator;

    @InjectMocks
    private UserService userService;

    @Nested
    class Create {
        @Test
        void givenValidRequest_whenCreateUser_thenReturnUser() {
            RegisterRequestDTO request = new RegisterRequestDTO(
                    "João",
                    "12345678900",
                    "joao@email.com",
                    "12345678",
                    1L,
                    2021,
                    2024
            );

            User user = User.builder()
                    .name("João")
                    .cpf("12345678900")
                    .email("email@email.com")
                    .password("12345678")
                    .build();

            UserSimpleDTO response = new UserSimpleDTO(
                    1L,
                    "João",
                    "joao@email.com"
            );


            Mockito.doNothing().when(userValidator).validateCreate(request);

            Mockito.when(userMapper.toEntity(request))
                    .thenReturn(user);

            Mockito.when(passwordEncoder.encode("12345678"))
                    .thenReturn("encryptedPassword");

            Mockito.when(userRepository.save(user))
                    .thenReturn(user);

            Mockito.when(userMapper.toSimpleDTO(user))
                    .thenReturn(response);

            UserSimpleDTO result = userService.createUser(request);

            Mockito.verify(passwordEncoder).encode("12345678");

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("João");
            assertThat(result.getEmail()).isEqualTo("joao@email.com");
            assertThat(user.getPassword()).isEqualTo("encryptedPassword");
        }

        @Test
        void givenExistingUser_whenCreateUser_thenThrowException() {
            RegisterRequestDTO request = new RegisterRequestDTO(
                    "João",
                    "12345678900",
                    "joao@email.com",
                    "12345678",
                    1L,
                    2021,
                    2024
            );

            Mockito.doThrow(new BusinessException("User already exists"))
                    .when(userValidator)
                    .validateCreate(request);

            assertThatThrownBy(() -> userService.createUser(request))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        void givenNullPassword_whenCreateUser_thenThrowException() {

            RegisterRequestDTO request = new RegisterRequestDTO(
                    "João",
                    "12345678900",
                    "joao@email.com",
                    null,
                    1L,
                    2021,
                    2024
            );

            Mockito.doThrow(new BusinessException("Password cannot be null"))
                    .when(userValidator)
                    .validateCreate(request);

            assertThatThrownBy(() -> userService.createUser(request))
                    .isInstanceOf(BusinessException.class);
        }
    }
}