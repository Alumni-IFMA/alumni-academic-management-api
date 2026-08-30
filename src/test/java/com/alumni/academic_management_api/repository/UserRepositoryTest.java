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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Set;

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
    class FindDistinctByAcademicProfilesCampusCourseIdInAndIdNotIn {

        private CampusCourse persistCampusCourse() {
            Campus campus = entityManager.persist(Campus.builder()
                    .name("Campus Central")
                    .city("São Luís")
                    .build());
            Course course = entityManager.persist(Course.builder()
                    .name("Engenharia")
                    .level(Level.GRADUACAO)
                    .modality(Modality.BACHARELADO)
                    .build());
            return entityManager.persist(CampusCourse.builder()
                    .campus(campus)
                    .course(course)
                    .build());
        }

        private User persistUserWithCampusCourse(String name, String email, CampusCourse campusCourse) {
            User user = entityManager.persist(User.builder()
                    .name(name)
                    .cpf(email)
                    .email(email)
                    .accountStatus(AccountStatus.ACTIVE)
                    .role(Role.ALUMNI)
                    .build());
            entityManager.persist(AcademicProfile.builder()
                    .user(user)
                    .campusCourse(campusCourse)
                    .entryYear(2020)
                    .conclusionYear(2024)
                    .build());
            return user;
        }

        @Test
        void givenUsersInSameCampusCourse_whenFindPaged_thenReturnPageExcludingGivenIds() {
            CampusCourse campusCourse = persistCampusCourse();
            User excludedUser = persistUserWithCampusCourse("Excluded", "excluded@test.com", campusCourse);
            User suggestedUser = persistUserWithCampusCourse("Suggested", "suggested@test.com", campusCourse);
            entityManager.flush();

            Page<User> result = userRepository.findDistinctByAcademicProfilesCampusCourseIdInAndIdNotIn(
                    Set.of(campusCourse.getId()),
                    Set.of(excludedUser.getId()),
                    PageRequest.of(0, 10)
            );

            assertThat(result.getContent()).containsExactly(suggestedUser);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        void givenMoreUsersThanPageSize_whenFindPaged_thenReturnRequestedPageOnly() {
            CampusCourse campusCourse = persistCampusCourse();
            User excludedUser = persistUserWithCampusCourse("Excluded", "excluded@test.com", campusCourse);
            persistUserWithCampusCourse("First", "first@test.com", campusCourse);
            persistUserWithCampusCourse("Second", "second@test.com", campusCourse);
            entityManager.flush();

            Page<User> result = userRepository.findDistinctByAcademicProfilesCampusCourseIdInAndIdNotIn(
                    Set.of(campusCourse.getId()),
                    Set.of(excludedUser.getId()),
                    PageRequest.of(0, 1)
            );

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(2);
            assertThat(result.getTotalPages()).isEqualTo(2);
        }
    }
}