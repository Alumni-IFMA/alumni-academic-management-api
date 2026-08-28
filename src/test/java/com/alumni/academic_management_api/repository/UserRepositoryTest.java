package com.alumni.academic_management_api.repository;

import com.alumni.academic_management_api.entity.AcademicProfile;
import com.alumni.academic_management_api.entity.Campus;
import com.alumni.academic_management_api.entity.CampusCourse;
import com.alumni.academic_management_api.entity.Course;
import com.alumni.academic_management_api.entity.User;
import com.alumni.academic_management_api.enums.AccountStatus;
import com.alumni.academic_management_api.enums.Level;
import com.alumni.academic_management_api.enums.Modality;
import com.alumni.academic_management_api.enums.Role;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

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
                    .accountStatus(AccountStatus.ACTIVE)
                    .role(Role.ALUMNI)
                    .build();

            User savedUser = userRepository.saveAndFlush(user);

            assertThat(savedUser.getId()).isNotNull();
            assertThat(user.getName()).isEqualTo(savedUser.getName());
            assertThat(user.getCpf()).isEqualTo(savedUser.getCpf());
            assertThat(user.getEmail()).isEqualTo(savedUser.getEmail());
            assertThat(user.getAccountStatus()).isEqualTo(savedUser.getAccountStatus());
        }

        @Test
        void givenUserWithoutName_whenSave_thenThrowException() {
            User user = User.builder()
                    .cpf("12345678900")
                    .email("email@email.com")
                    .accountStatus(AccountStatus.ACTIVE)
                    .role(Role.ALUMNI)
                    .build();

            assertThatThrownBy(() -> userRepository.saveAndFlush(user))
                    .isInstanceOf(Exception.class);
        }

        @Test
        void givenUserWithRoleAlumni_whenSave_thenRoleIsPersistedCorrectly() {
            User user = User.builder()
                    .name("Maria Souza")
                    .cpf("98765432100")
                    .email("maria@email.com")
                    .accountStatus(AccountStatus.PENDING_VERIFICATION)
                    .role(Role.ALUMNI)
                    .build();

            User savedUser = userRepository.saveAndFlush(user);
            entityManager.clear();

            User found = userRepository.findById(savedUser.getId()).orElseThrow();

            assertThat(found.getRole()).isEqualTo(Role.ALUMNI);
        }
    }

    @Nested
    class SearchUsers {

        @Test
        void givenOnlyCampusId_whenSearchUsers_thenReturnUsersFromThatCampus() {
            Campus campus = entityManager.persistAndFlush(
                    Campus.builder().name("Campus Centro").city("São Luís").build());
            Course course = entityManager.persistAndFlush(Course.builder()
                    .name("Computação").level(Level.GRADUACAO).modality(Modality.BACHARELADO).build());
            CampusCourse campusCourse = entityManager.persistAndFlush(
                    CampusCourse.builder().campus(campus).course(course).build());
            User user = entityManager.persistAndFlush(User.builder()
                    .name("João Silva").cpf("12345678901").email("joao.search@email.com")
                    .accountStatus(AccountStatus.ACTIVE).role(Role.ALUMNI).build());
            entityManager.persistAndFlush(AcademicProfile.builder()
                    .user(user).campusCourse(campusCourse).entryYear(2020).conclusionYear(2024).build());
            User inactiveUser = entityManager.persistAndFlush(User.builder()
                    .name("Usuário Inativo").cpf("12345678903").email("inactive.search@email.com")
                    .accountStatus(AccountStatus.SUSPENDED).role(Role.ALUMNI).build());
            entityManager.persistAndFlush(AcademicProfile.builder()
                    .user(inactiveUser).campusCourse(campusCourse).entryYear(2020).conclusionYear(2024).build());

            List<User> result = userRepository.searchUsers(campus.getId(), null, null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("João Silva");
        }

        @Test
        void givenOnlyCourseId_whenSearchUsers_thenReturnUsersFromThatCourse() {
            Campus campus = entityManager.persistAndFlush(
                    Campus.builder().name("Campus Centro").city("São Luís").build());
            Course course = entityManager.persistAndFlush(Course.builder()
                    .name("Computação").level(Level.GRADUACAO).modality(Modality.BACHARELADO).build());
            CampusCourse campusCourse = entityManager.persistAndFlush(
                    CampusCourse.builder().campus(campus).course(course).build());
            User user = entityManager.persistAndFlush(User.builder()
                    .name("Maria Souza").cpf("12345678902").email("maria.search@email.com")
                    .accountStatus(AccountStatus.ACTIVE).role(Role.ALUMNI).build());
            entityManager.persistAndFlush(AcademicProfile.builder()
                    .user(user).campusCourse(campusCourse).entryYear(2020).conclusionYear(2024).build());

            List<User> result = userRepository.searchUsers(null, course.getId(), null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Maria Souza");
        }
    }
}
