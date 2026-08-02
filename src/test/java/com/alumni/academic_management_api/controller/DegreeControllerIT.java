package com.alumni.academic_management_api.controller;

import com.alumni.academic_management_api.dto.auth.LoginRequestDTO;
import com.alumni.academic_management_api.entity.Degree;
import com.alumni.academic_management_api.entity.User;
import com.alumni.academic_management_api.enums.AccountStatus;
import com.alumni.academic_management_api.enums.Role;
import com.alumni.academic_management_api.repository.DegreeRepository;
import com.alumni.academic_management_api.repository.UserRepository;
import com.alumni.academic_management_api.service.FileStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.MinioClient;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DegreeControllerIT {

    private static final String BASE_URL = "/degrees";
    private static final String PASSWORD = "senha12345";

    @MockBean
    private MinioClient minioClient;

    @MockBean
    private FileStorageService fileStorageService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DegreeRepository degreeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        degreeRepository.deleteAll();
        userRepository.deleteAll();
    }

    private User saveUser(String email, Role role, String cpf) {
        return userRepository.save(User.builder()
                .name("Test User")
                .cpf(cpf)
                .email(email)
                .password(passwordEncoder.encode(PASSWORD))
                .accountStatus(AccountStatus.ACTIVE)
                .role(role)
                .build());
    }

    private String loginAndGetToken(String email, Role role, String cpf) throws Exception {
        saveUser(email, role, cpf);

        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequestDTO(email, PASSWORD))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("token").asText();
    }

    @Nested
    class Upload {

        @Test
        void givenNoToken_whenUpload_thenReturn401() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "diploma.pdf", "application/pdf", new byte[]{1, 2, 3}
            );

            mockMvc.perform(multipart(BASE_URL)
                            .file(file)
                            .param("userId", "1")
                            .param("title", "Bacharelado"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void givenAlumniToken_whenUpload_thenReturn403() throws Exception {
            String token = loginAndGetToken("alumni@test.com", Role.ALUMNI, "11111111111");
            MockMultipartFile file = new MockMultipartFile(
                    "file", "diploma.pdf", "application/pdf", new byte[]{1, 2, 3}
            );

            mockMvc.perform(multipart(BASE_URL)
                            .file(file)
                            .param("userId", "1")
                            .param("title", "Bacharelado")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
        }

        @Test
        void givenAdminToken_whenUploadValidPdfForExistingUser_thenReturn201WithFileUrl() throws Exception {
            String adminToken = loginAndGetToken("admin@test.com", Role.ADMIN, "22222222222");
            User target = saveUser("target@test.com", Role.ALUMNI, "33333333333");
            String expectedUrl = "http://localhost:9000/alumni-files/diplomas/uuid.pdf";
            when(fileStorageService.uploadFile(any(), any())).thenReturn(expectedUrl);
            MockMultipartFile file = new MockMultipartFile(
                    "file", "diploma.pdf", "application/pdf", new byte[]{1, 2, 3}
            );

            mockMvc.perform(multipart(BASE_URL)
                            .file(file)
                            .param("userId", String.valueOf(target.getId()))
                            .param("title", "Bacharelado em Ciência da Computação")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.fileUrl").value(expectedUrl))
                    .andExpect(jsonPath("$.userId").value(target.getId()));
        }

        @Test
        void givenAdminToken_whenUploadNonPdfFile_thenReturn400() throws Exception {
            String adminToken = loginAndGetToken("admin2@test.com", Role.ADMIN, "44444444444");
            User target = saveUser("target2@test.com", Role.ALUMNI, "55555555555");
            MockMultipartFile file = new MockMultipartFile(
                    "file", "diploma.jpg", "image/jpeg", new byte[]{1}
            );

            mockMvc.perform(multipart(BASE_URL)
                            .file(file)
                            .param("userId", String.valueOf(target.getId()))
                            .param("title", "Bacharelado")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void givenAdminToken_whenUploadForNonExistentUser_thenReturn404() throws Exception {
            String adminToken = loginAndGetToken("admin3@test.com", Role.ADMIN, "66666666666");
            MockMultipartFile file = new MockMultipartFile(
                    "file", "diploma.pdf", "application/pdf", new byte[]{1}
            );

            mockMvc.perform(multipart(BASE_URL)
                            .file(file)
                            .param("userId", "999999")
                            .param("title", "Bacharelado")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class FindMine {

        @Test
        void givenNoToken_whenFindMine_thenReturn401() throws Exception {
            mockMvc.perform(get(BASE_URL + "/me"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void givenAuthenticatedUserWithOwnDegree_whenFindMine_thenReturnOnlyOwnDegrees() throws Exception {
            String token = loginAndGetToken("owner@test.com", Role.ALUMNI, "77777777777");
            User owner = userRepository.findByEmail("owner@test.com").orElseThrow();
            User other = saveUser("other@test.com", Role.ALUMNI, "88888888888");

            degreeRepository.save(Degree.builder()
                    .title("Diploma do Owner")
                    .fileUrl("http://localhost:9000/alumni-files/diplomas/owner.pdf")
                    .user(owner)
                    .build());
            degreeRepository.save(Degree.builder()
                    .title("Diploma de Outro Usuário")
                    .fileUrl("http://localhost:9000/alumni-files/diplomas/other.pdf")
                    .user(other)
                    .build());

            mockMvc.perform(get(BASE_URL + "/me")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].title").value("Diploma do Owner"));
        }
    }

    @Nested
    class Download {

        @Test
        void givenNoToken_whenDownload_thenReturn401() throws Exception {
            mockMvc.perform(get(BASE_URL + "/{id}/download", 1L))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void givenOwner_whenDownload_thenReturn200WithDownloadUrl() throws Exception {
            String token = loginAndGetToken("owner2@test.com", Role.ALUMNI, "99999999999");
            User owner = userRepository.findByEmail("owner2@test.com").orElseThrow();
            Degree degree = degreeRepository.save(Degree.builder()
                    .title("Diploma")
                    .fileUrl("http://localhost:9000/alumni-files/diplomas/x.pdf")
                    .user(owner)
                    .build());
            String presignedUrl = "http://localhost:9000/alumni-files/diplomas/x.pdf?X-Amz-Signature=xyz";
            when(fileStorageService.generatePresignedUrl(degree.getFileUrl(), 15)).thenReturn(presignedUrl);

            mockMvc.perform(get(BASE_URL + "/{id}/download", degree.getId())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.downloadUrl").value(presignedUrl));
        }

        @Test
        void givenNonOwner_whenDownload_thenReturn400() throws Exception {
            User owner = saveUser("owner3@test.com", Role.ALUMNI, "10101010101");
            String otherToken = loginAndGetToken("intruder@test.com", Role.ALUMNI, "12121212121");
            Degree degree = degreeRepository.save(Degree.builder()
                    .title("Diploma")
                    .fileUrl("http://localhost:9000/alumni-files/diplomas/x.pdf")
                    .user(owner)
                    .build());

            mockMvc.perform(get(BASE_URL + "/{id}/download", degree.getId())
                            .header("Authorization", "Bearer " + otherToken))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void givenNonExistentDegree_whenDownload_thenReturn404() throws Exception {
            String token = loginAndGetToken("user4@test.com", Role.ALUMNI, "13131313131");

            mockMvc.perform(get(BASE_URL + "/{id}/download", 999999L)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound());
        }
    }
}
