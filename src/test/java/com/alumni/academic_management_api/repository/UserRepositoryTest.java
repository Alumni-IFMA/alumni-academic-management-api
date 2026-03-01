package com.alumni.academic_management_api.repository;

import com.alumni.academic_management_api.entity.User;
import com.alumni.academic_management_api.enums.AccountStatus;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Nested
    class Save {

        @Test
        void givenUser_whenSave_thenReturnSavedUser() {
            User user = User.builder()
                    .name("João Silva")
                    .cpf("12345678900")
                    .email("joao@email.com")
                    .password("12345678")
                    .accountStatus(AccountStatus.ACTIVE)
                    .build();

            User savedUser = userRepository.saveAndFlush(user);

            assertThat(savedUser.getId()).isNotNull();
            assertThat(user.getName()).isEqualTo(savedUser.getName());
            assertThat(user.getCpf()).isEqualTo(savedUser.getCpf());
            assertThat(user.getEmail()).isEqualTo(savedUser.getEmail());
            assertThat(user.getPassword()).isEqualTo(savedUser.getPassword());
            assertThat(user.getAccountStatus()).isEqualTo(savedUser.getAccountStatus());
        }

        @Test
        void givenUserWithoutName_whenSave_thenThrowException() {
            User user = User.builder()
                    .cpf("12345678900")
                    .email("email@email.com")
                    .password("12345678")
                    .accountStatus(AccountStatus.ACTIVE)
                    .build();

            assertThatThrownBy(() -> userRepository.saveAndFlush(user))
                    .isInstanceOf(Exception.class);
        }
    }
}