package com.alumni.academic_management_api.service;

import com.alumni.academic_management_api.dto.user.RegisterRequestDTO;
import com.alumni.academic_management_api.dto.user.UserSimpleDTO;
import com.alumni.academic_management_api.entity.User;
import com.alumni.academic_management_api.enums.AccountStatus;
import com.alumni.academic_management_api.enums.Role;
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
                    AccountStatus.PENDING_VERIFICATION,
                    Role.ALUMNI
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
            assertThat(result.getRole()).isEqualTo(Role.ALUMNI);
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
                    AccountStatus.PENDING_VERIFICATION,
                    Role.ALUMNI
            );

            Mockito.when(userRepository.findAll()).thenReturn(List.of(user));
            Mockito.when(userMapper.toSimpleDTOList(List.of(user))).thenReturn(List.of(dto));

            List<UserSimpleDTO> result = userService.findAll();

            assertThat(result).isNotNull().hasSize(1);
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
                    AccountStatus.PENDING_VERIFICATION,
                    Role.ALUMNI
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

    @Nested
    class GetUserProfile {
        @Test
        void givenNonExistingUser_whenGetUserProfile_thenThrowResourceNotFoundException() {
            Mockito.when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUserProfile(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    @Nested
    class UpdateUserRole {

        @Test
        void givenAdminUpdatingOtherUser_whenUpdateUserRole_thenReturnUpdatedUser() {
            Long targetId = 2L;
            String adminEmail = "admin@test.com";

            User targetUser = User.builder()
                    .name("Target")
                    .email("target@test.com")
                    .cpf("99999999999")
                    .accountStatus(AccountStatus.PENDING_VERIFICATION)
                    .role(Role.ALUMNI)
                    .build();

            UserSimpleDTO expectedDTO = new UserSimpleDTO(
                    targetId,
                    "Target",
                    "target@test.com",
                    List.of(),
                    AccountStatus.PENDING_VERIFICATION,
                    Role.ADMIN
            );

            Mockito.when(userRepository.findById(targetId)).thenReturn(Optional.of(targetUser));
            Mockito.when(userRepository.save(targetUser)).thenReturn(targetUser);
            Mockito.when(userMapper.toSimpleDTO(targetUser)).thenReturn(expectedDTO);

            UserSimpleDTO result = userService.updateUserRole(targetId, Role.ADMIN, adminEmail);

            assertThat(result.getRole()).isEqualTo(Role.ADMIN);
            Mockito.verify(userRepository).save(targetUser);
        }

        @Test
        void givenAdminUpdatingOwnRole_whenUpdateUserRole_thenThrowBusinessException() {
            Long targetId = 1L;
            String adminEmail = "admin@test.com";

            User adminUser = User.builder()
                    .name("Admin")
                    .email(adminEmail)
                    .cpf("11111111100")
                    .accountStatus(AccountStatus.ACTIVE)
                    .role(Role.ADMIN)
                    .build();

            Mockito.when(userRepository.findById(targetId)).thenReturn(Optional.of(adminUser));

            assertThatThrownBy(() -> userService.updateUserRole(targetId, Role.ALUMNI, adminEmail))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("cannot change their own role");

            Mockito.verify(userRepository, Mockito.never()).save(Mockito.any());
        }

        @Test
        void givenNonExistingUser_whenUpdateUserRole_thenThrowResourceNotFoundException() {
            Mockito.when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.updateUserRole(99L, Role.ADMIN, "admin@test.com"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");

            Mockito.verify(userRepository, Mockito.never()).save(Mockito.any());
        }
    }
}