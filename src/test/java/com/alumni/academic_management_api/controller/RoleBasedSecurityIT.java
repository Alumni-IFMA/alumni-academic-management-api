package com.alumni.academic_management_api.controller;

import com.alumni.academic_management_api.dto.auth.LoginRequestDTO;
import com.alumni.academic_management_api.dto.user.UpdateRoleRequestDTO;
import com.alumni.academic_management_api.entity.User;
import com.alumni.academic_management_api.enums.AccountStatus;
import com.alumni.academic_management_api.enums.Role;
import com.alumni.academic_management_api.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import io.minio.MinioClient;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import jakarta.transaction.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RoleBasedSecurityIT {

    @MockBean
    private MinioClient minioClient;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String PASSWORD = "senha12345";

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    private String loginAndGetToken(String email, Role role, String cpf) throws Exception {
        userRepository.save(User.builder()
                .name("Test User")
                .cpf(cpf)
                .email(email)
                .password(passwordEncoder.encode(PASSWORD))
                .accountStatus(AccountStatus.ACTIVE)
                .role(role)
                .build());

        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequestDTO(email, PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("token").asText();
    }

    @Nested
    class AdminEndpoint {

        private static final String URL = "/auth/users";

        @Test
        void givenNoToken_whenGetAllUsers_thenReturn401() throws Exception {
            mockMvc.perform(get(URL))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void givenAlumniToken_whenGetAllUsers_thenReturn403() throws Exception {
            String token = loginAndGetToken("alumni@test.com", Role.ALUMNI, "11111111111");

            mockMvc.perform(get(URL)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
        }

        @Test
        void givenAdminToken_whenGetAllUsers_thenReturn200() throws Exception {
            String token = loginAndGetToken("admin@test.com", Role.ADMIN, "22222222222");

            mockMvc.perform(get(URL)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }

    @Nested
    class ProtectedEndpoint {

        @Test
        void givenNoToken_whenGetUserById_thenReturn401() throws Exception {
            mockMvc.perform(get("/auth/users/{id}", 999L))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void givenAuthenticatedAlumni_whenGetUserById_thenReturn200() throws Exception {
            User user = userRepository.save(User.builder()
                    .name("Alumni User")
                    .cpf("33333333333")
                    .email("alumni2@test.com")
                    .password(passwordEncoder.encode(PASSWORD))
                    .accountStatus(AccountStatus.ACTIVE)
                    .role(Role.ALUMNI)
                    .build());

            String response = mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new LoginRequestDTO("alumni2@test.com", PASSWORD))))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            String token = objectMapper.readTree(response).get("token").asText();

            mockMvc.perform(get("/auth/users/{id}", user.getId())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    class UpdateUserRole {

        private static final String URL = "/auth/users/{id}/role";

        @Test
        void givenAdminToken_whenUpdateOtherUserRole_thenReturn200AndRoleChanged() throws Exception {
            String adminToken = loginAndGetToken("admin@test.com", Role.ADMIN, "44444444441");

            User target = userRepository.save(User.builder()
                    .name("Target User")
                    .cpf("55555555555")
                    .email("target@test.com")
                    .password(passwordEncoder.encode(PASSWORD))
                    .accountStatus(AccountStatus.ACTIVE)
                    .role(Role.ALUMNI)
                    .build());

            mockMvc.perform(patch(URL, target.getId())
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new UpdateRoleRequestDTO(Role.ADMIN))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.role").value("ADMIN"));

            User updatedUser = userRepository.findById(target.getId()).orElseThrow();
            assertThat(updatedUser.getRole()).isEqualTo(Role.ADMIN);
        }

        @Test
        void givenAdminToken_whenUpdateOwnRole_thenReturn400() throws Exception {
            String adminToken = loginAndGetToken("admin@test.com", Role.ADMIN, "44444444442");
            Long adminId = userRepository.findByEmail("admin@test.com").orElseThrow().getId();

            mockMvc.perform(patch(URL, adminId)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new UpdateRoleRequestDTO(Role.ALUMNI))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void givenAlumniToken_whenUpdateRole_thenReturn403() throws Exception {
            String alumniToken = loginAndGetToken("alumni@test.com", Role.ALUMNI, "66666666666");

            User target = userRepository.save(User.builder()
                    .name("Target User")
                    .cpf("77777777777")
                    .email("target2@test.com")
                    .password(passwordEncoder.encode(PASSWORD))
                    .accountStatus(AccountStatus.ACTIVE)
                    .role(Role.ALUMNI)
                    .build());

            mockMvc.perform(patch(URL, target.getId())
                            .header("Authorization", "Bearer " + alumniToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new UpdateRoleRequestDTO(Role.ADMIN))))
                    .andExpect(status().isForbidden());
        }

        @Test
        void givenNoToken_whenUpdateRole_thenReturn401() throws Exception {
            mockMvc.perform(patch(URL, 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new UpdateRoleRequestDTO(Role.ADMIN))))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void givenAdminToken_whenUpdateNonExistentUser_thenReturn404() throws Exception {
            String adminToken = loginAndGetToken("admin@test.com", Role.ADMIN, "44444444443");

            mockMvc.perform(patch(URL, 999999L)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new UpdateRoleRequestDTO(Role.ADMIN))))
                    .andExpect(status().isNotFound());
        }
    }
}