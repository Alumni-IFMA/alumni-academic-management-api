package com.alumni.academic_management_api.service;

import com.alumni.academic_management_api.dto.user.RegisterRequestDTO;
import com.alumni.academic_management_api.dto.user.UserSimpleDTO;
import com.alumni.academic_management_api.entity.User;
import com.alumni.academic_management_api.enums.AccountStatus;
import com.alumni.academic_management_api.exception.BusinessException;
import com.alumni.academic_management_api.exception.ResourceNotFoundException;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

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
                    1L,
                    2021,
                    2024
            );

            User user = User.builder()
                    .name("João")
                    .cpf("12345678900")
                    .email("email@email.com")
                    .password("12345678")
                    .accountStatus(AccountStatus.PENDING_VERIFICATION)
                    .build();

            UserSimpleDTO response = new UserSimpleDTO(
                    1L,
                    "João",
                    "joao@email.com",
                    List.of(),
                    AccountStatus.PENDING_VERIFICATION
            );


            Mockito.doNothing().when(userValidator).validateCreate(request);

            Mockito.when(userMapper.toEntity(request))
                    .thenReturn(user);

            Mockito.when(userRepository.save(user))
                    .thenReturn(user);

            Mockito.when(userMapper.toSimpleDTO(user))
                    .thenReturn(response);

            UserSimpleDTO result = userService.createUser(request);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("João");
            assertThat(result.getEmail()).isEqualTo("joao@email.com");
        }

        @Test
        void givenExistingUser_whenCreateUser_thenThrowException() {
            RegisterRequestDTO request = new RegisterRequestDTO(
                    "João",
                    "12345678900",
                    "joao@email.com",
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
    }

    @Nested
    class FindAll {
        @Test
        void givenUsersExist_whenFindAll_thenReturnList() {
            User user = User.builder()
                    .name("João")
                    .email("joao@email.com")
                    .build();

            UserSimpleDTO dto = new UserSimpleDTO(
                    1L,
                    "João",
                    "joao@email.com",
                    List.of(),
                    AccountStatus.PENDING_VERIFICATION
            );

            Mockito.when(userRepository.findAll()).thenReturn(List.of(user));
            Mockito.when(userMapper.toSimpleDTOList(List.of(user))).thenReturn(List.of(dto));

            List<UserSimpleDTO> result = userService.findAll();

            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("João");
        }

        @Test
        void givenNoUsers_whenFindAll_thenReturnEmptyList() {
            Mockito.when(userRepository.findAll()).thenReturn(List.of());
            Mockito.when(userMapper.toSimpleDTOList(List.of())).thenReturn(List.of());

            List<UserSimpleDTO> result = userService.findAll();

            assertThat(result)
                    .isNotNull()
                    .isEmpty();
        }
    }


    @Nested
    class FindById {
        @Test
        void givenValidRequest_whenFindById_thenReturnUser() {

            Long userId = 1L;

            User user = User.builder()
                    .name("João")
                    .cpf("12345678900")
                    .email("email@email.com")
                    .password("12345678")
                    .build();

            UserSimpleDTO response = new UserSimpleDTO(
                    1L,
                    "João",
                    "joao@email.com",
                    List.of(),
                    AccountStatus.PENDING_VERIFICATION
            );

            Mockito.when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            Mockito.when(userMapper.toSimpleDTO(user)).thenReturn(response);

            UserSimpleDTO result = userService.findUserById(userId);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("João");
            assertThat(result.getEmail()).isEqualTo("joao@email.com");

            Mockito.verify(userRepository).findById(userId);
            Mockito.verify(userMapper).toSimpleDTO(user);
        }


        @Test
        void givenNonExistingUser_whenFindById_thenThrowException() {

            Long userId = 1L;

            Mockito.when(userRepository.findById(userId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.findUserById(userId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found");

            Mockito.verify(userRepository).findById(userId);
            Mockito.verify(userMapper, Mockito.never()).toSimpleDTO(Mockito.any());
        }
    }
}