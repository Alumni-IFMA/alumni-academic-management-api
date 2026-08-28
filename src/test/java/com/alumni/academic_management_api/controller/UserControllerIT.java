package com.alumni.academic_management_api.controller;

import com.alumni.academic_management_api.dto.user.RegisterRequestDTO;
import com.alumni.academic_management_api.entity.AcademicProfile;
import com.alumni.academic_management_api.entity.CampusCourse;
import com.alumni.academic_management_api.entity.Campus;
import com.alumni.academic_management_api.entity.Course;
import com.alumni.academic_management_api.entity.User;
import com.alumni.academic_management_api.enums.AccountStatus;
import com.alumni.academic_management_api.enums.Role;
import com.alumni.academic_management_api.enums.Level;
import com.alumni.academic_management_api.enums.Modality;
import com.alumni.academic_management_api.repository.AcademicProfileRepository;
import com.alumni.academic_management_api.repository.CampusesCourseRepository;
import com.alumni.academic_management_api.repository.UserRepository;
import com.alumni.academic_management_api.service.FileStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.MinioClient;
import jakarta.transaction.Transactional;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerIT {

    @MockBean
    private MinioClient minioClient;

    @MockBean
    private FileStorageService fileStorageService;

    @MockitoBean
    private CampusesCourseRepository campusesCourseRepository;

    @MockitoBean
    private AcademicProfileRepository academicProfileRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;


    @Nested
    class createUser {

        private static final String URL = "/auth/register";

        @Test
        void givenValidUserRequest_whenCreate_thenReturnCreated() throws Exception {
            CampusCourse course = new CampusCourse();

            RegisterRequestDTO requestDTO = RegisterRequestDTO.builder()
                    .name("João Silva")
                    .cpf("12345678900")
                    .email("joao@gmail.com")
                    .campusCourseId(1L)
                    .entryYear(2021)
                    .conclusionYear(2024)
                    .build();
            course.setId(1L);

            Mockito.when(campusesCourseRepository.findById(1L))
                    .thenReturn(Optional.of(course));

            Mockito.when(academicProfileRepository.save(Mockito.any()))
                    .thenReturn(new AcademicProfile());


            mockMvc.perform(post(URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("João Silva"))
                    .andExpect(jsonPath("$.email").value("joao@gmail.com"))
                    .andExpect(jsonPath("$.role").value("ALUMNI"));

        }

        @Test
        void givenInvalidCampusCourse_whenCreate_thenReturnNotFound() throws Exception {
            RegisterRequestDTO requestDTO = RegisterRequestDTO.builder()
                    .name("Erro Silva")
                    .cpf("12345678900")
                    .email("erro@gmail.com")
                    .campusCourseId(99L)
                    .entryYear(2021)
                    .conclusionYear(2024)
                    .build();
            Mockito.when(campusesCourseRepository.findById(99L))
                    .thenReturn(Optional.empty());


            mockMvc.perform(post(URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class FindAll {

        private static final String URL = "/auth/users";

        @Test
        @WithMockUser(roles = "ADMIN")
        void givenUsersExist_whenFindAll_thenReturn200WithList() throws Exception {
            mockMvc.perform(get(URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }

    @Nested
    class SearchByName {

        private static final String URL = "/auth/users/search";

        @Test
        @WithMockUser
        void givenNameFragment_whenSearch_thenReturnMatchingUsers() throws Exception {
            userRepository.save(User.builder()
                    .name("João da Silva")
                    .cpf("12345678901")
                    .email("joao.search@test.com")
                    .accountStatus(AccountStatus.ACTIVE)
                    .role(Role.ALUMNI)
                    .build());
            userRepository.save(User.builder()
                    .name("Maria Souza")
                    .cpf("12345678902")
                    .email("maria.search@test.com")
                    .accountStatus(AccountStatus.ACTIVE)
                    .role(Role.ALUMNI)
                    .build());

            mockMvc.perform(get(URL).param("name", "joão"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].name").value("João da Silva"));
        }

        @Test
        @WithMockUser
        void givenCampusAndCourse_whenSearch_thenReturnOnlyUsersFromThatAcademicProfile() throws Exception {
            Campus campus = Campus.builder().name("Campus Centro").city("São Luís").build();
            Course course = Course.builder().name("Computação").level(Level.GRADUACAO).modality(Modality.BACHARELADO).build();
            entityManager.persist(campus);
            entityManager.persist(course);
            CampusCourse campusCourse = CampusCourse.builder().campus(campus).course(course).build();
            entityManager.persist(campusCourse);

            User matchingUser = userRepository.save(User.builder()
                    .name("Ana Lima")
                    .cpf("12345678903")
                    .email("ana.search@test.com")
                    .accountStatus(AccountStatus.ACTIVE)
                    .role(Role.ALUMNI)
                    .build());
            User suspendedUser = userRepository.save(User.builder()
                    .name("Ana Suspensa")
                    .cpf("12345678904")
                    .email("ana.suspended@test.com")
                    .accountStatus(AccountStatus.SUSPENDED)
                    .role(Role.ALUMNI)
                    .build());
            entityManager.persist(AcademicProfile.builder()
                    .user(matchingUser).campusCourse(campusCourse).entryYear(2020).conclusionYear(2024).build());
            entityManager.persist(AcademicProfile.builder()
                    .user(suspendedUser).campusCourse(campusCourse).entryYear(2020).conclusionYear(2024).build());
            entityManager.flush();
            entityManager.clear();

            mockMvc.perform(get(URL)
                            .param("campusId", campus.getId().toString())
                            .param("courseId", course.getId().toString())
                            .param("name", "ana"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].name").value("Ana Lima"))
                    .andExpect(jsonPath("$[0].campus").value("Campus Centro"))
                    .andExpect(jsonPath("$[0].course").value("Computação"));
        }

        @Test
        void givenNoToken_whenSearch_thenReturn401() throws Exception {
            mockMvc.perform(get(URL).param("name", "joão"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class GetUserProfile {

        private static final String URL = "/auth/users/{id}/profile";

        @Test
        @WithMockUser
        void givenExistingUser_whenGetProfile_thenReturn200WithProfileData() throws Exception {
            CampusCourse mockCourse = new CampusCourse();

            RegisterRequestDTO requestDTO = RegisterRequestDTO.builder()
                    .name("Maria Silva")
                    .cpf("11122233344")
                    .email("maria@gmail.com")
                    .campusCourseId(1L)
                    .entryYear(2020)
                    .conclusionYear(2023)
                    .build();

            mockCourse.setId(1L);

            Mockito.when(campusesCourseRepository.findById(1L))
                    .thenReturn(Optional.of(mockCourse));

            Mockito.when(academicProfileRepository.save(Mockito.any()))
                    .thenReturn(new AcademicProfile());

            String responseBody = mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();

            Long userId = objectMapper.readTree(responseBody).get("id").asLong();

            mockMvc.perform(get(URL, userId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(userId))
                    .andExpect(jsonPath("$.name").value("Maria Silva"))
                    .andExpect(jsonPath("$.email").value("maria@gmail.com"));
        }

        @Test
        @WithMockUser
        void givenNonExistingUser_whenGetProfile_thenReturn404() throws Exception {
            mockMvc.perform(get(URL, 999999L))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class FindUserById {

        private static final String URL = "/auth/users";


        @Test
        @WithMockUser
        void givenValidUserRequest_whenFindById_thenReturnUser() throws Exception {
            User user = User.builder()
                    .name("João Silva")
                    .email("joao@gmail.com")
                    .cpf("12345678900")
                    .accountStatus(AccountStatus.PENDING_VERIFICATION)
                    .role(Role.ALUMNI)
                    .build();

            User savedUser = userRepository.save(user);

            mockMvc.perform(get(URL + "/" + savedUser.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("João Silva"))
                    .andExpect(jsonPath("$.email").value("joao@gmail.com"));
        }

        @Test
        @WithMockUser
        void givenInvalidUserRequest_whenFindById_thenReturnNotFound() throws Exception {
            mockMvc.perform(get("/auth/users/{id}", 999L))
                    .andExpect(status().isNotFound())
                    .andExpect(content().string(containsString("User not found")));
        }
    }

    @Nested
    class UploadProfilePicture {

        private static final String URL = "/auth/users/{id}/profile-picture";

        @Test
        void givenNoToken_whenUploadProfilePicture_thenReturn401() throws Exception {
            User user = userRepository.save(User.builder()
                    .name("Test User")
                    .cpf("10000000001")
                    .email("upload401@test.com")
                    .accountStatus(AccountStatus.PENDING_VERIFICATION)
                    .role(Role.ALUMNI)
                    .build());
            MockMultipartFile file = new MockMultipartFile(
                    "file", "avatar.jpg", "image/jpeg", new byte[]{1, 2, 3}
            );

            mockMvc.perform(multipart(URL, user.getId()).file(file))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser
        void givenAuthenticatedUser_whenUploadProfilePicture_thenReturn200WithUrl() throws Exception {
            User user = userRepository.save(User.builder()
                    .name("Test User")
                    .cpf("10000000002")
                    .email("upload200@test.com")
                    .accountStatus(AccountStatus.PENDING_VERIFICATION)
                    .role(Role.ALUMNI)
                    .build());
            String expectedUrl = "http://localhost:9000/alumni-files/profile-pictures/uuid.jpg";
            Mockito.when(fileStorageService.uploadFile(any(), any())).thenReturn(expectedUrl);
            MockMultipartFile file = new MockMultipartFile(
                    "file", "avatar.jpg", "image/jpeg", new byte[]{1, 2, 3}
            );

            mockMvc.perform(multipart(URL, user.getId()).file(file))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.profilePictureUrl").value(expectedUrl));
        }

        @Test
        @WithMockUser
        void givenAuthenticatedUser_whenUploadProfilePictureForNonExistentUser_thenReturn404() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "avatar.jpg", "image/jpeg", new byte[]{1}
            );

            mockMvc.perform(multipart(URL, 999999L).file(file))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class CompleteOnboarding {

        private static final String URL = "/auth/users/{id}/onboarding";

        @Test
        @WithMockUser(username = "joao@gmail.com")
        void givenValidUserAndCorrectEmail_whenCompleteOnboarding_thenReturn204AndSaveInDb() throws Exception {
            User user = User.builder()
                    .name("João Silva")
                    .cpf("98765432100")
                    .email("joao@gmail.com")
                    .accountStatus(AccountStatus.ACTIVE)
                    .role(Role.ALUMNI)
                    .hasSeenTutorial(false)
                    .build();
            User savedUser = userRepository.save(user);

            mockMvc.perform(patch(URL, savedUser.getId()))
                    .andExpect(status().isNoContent());

            User updatedUser = userRepository.findById(savedUser.getId()).orElseThrow();
            assertThat(updatedUser.getHasSeenTutorial()).isTrue();
        }

        @Test
        @WithMockUser(username = "ricardo@gmail.com")
        void givenDifferentUserEmail_whenCompleteOnboarding_thenReturn400() throws Exception {
            User user = User.builder()
                    .name("Maria Silva")
                    .cpf("98765432101")
                    .email("maria@gmail.com")
                    .accountStatus(AccountStatus.ACTIVE)
                    .role(Role.ALUMNI)
                    .hasSeenTutorial(false)
                    .build();
            User savedUser = userRepository.save(user);

            mockMvc.perform(patch(URL, savedUser.getId()))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(containsString("You do not have permission")));
        }

        @Test
        @WithMockUser(username = "joao@gmail.com")
        void givenNonExistingUser_whenCompleteOnboarding_thenReturn404() throws Exception {
            mockMvc.perform(patch(URL, 999999L))
                    .andExpect(status().isNotFound())
                    .andExpect(content().string(containsString("User not found")));
        }

        @Test
        void givenNoToken_whenCompleteOnboarding_thenReturn401() throws Exception {
            mockMvc.perform(patch(URL, 1L))
                    .andExpect(status().isUnauthorized());
        }
    }
}
