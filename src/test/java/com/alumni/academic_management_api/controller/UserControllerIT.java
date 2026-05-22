package com.alumni.academic_management_api.controller;

import com.alumni.academic_management_api.dto.user.RegisterRequestDTO;
import com.alumni.academic_management_api.entity.User;
import com.alumni.academic_management_api.enums.AccountStatus;
import com.alumni.academic_management_api.enums.Role;
import com.alumni.academic_management_api.repository.UserRepository;
import com.alumni.academic_management_api.service.FileStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.MinioClient;
import jakarta.transaction.Transactional;
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
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Nested
    class createUser {

        private static final String URL = "/auth/register";

        @Test
        void givenValidUserRequest_whenCreate_thenReturnCreated() throws Exception {
            RegisterRequestDTO requestDTO = RegisterRequestDTO.builder()
                    .name("João Silva")
                    .cpf("12345678900")
                    .email("joao@gmail.com")
                    .campusCourseId(1L)
                    .entryYear(2021)
                    .conclusionYear(2024)
                    .build();

            mockMvc.perform(post(URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDTO)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("João Silva"))
                    .andExpect(jsonPath("$.email").value("joao@gmail.com"))
                    .andExpect(jsonPath("$.role").value("ALUMNI"));

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
    class GetUserProfile {

        private static final String URL = "/auth/users/{id}/profile";

        @Test
        @WithMockUser
        void givenExistingUser_whenGetProfile_thenReturn200WithProfileData() throws Exception {
            RegisterRequestDTO requestDTO = RegisterRequestDTO.builder()
                    .name("Maria Silva")
                    .cpf("11122233344")
                    .email("maria@gmail.com")
                    .campusCourseId(1L)
                    .entryYear(2020)
                    .conclusionYear(2023)
                    .build();

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
}
