package com.alumni.academic_management_api.controller;

import com.alumni.academic_management_api.dto.auth.LoginRequestDTO;
import com.alumni.academic_management_api.dto.job.JobRequestDTO;
import com.alumni.academic_management_api.entity.Job;
import com.alumni.academic_management_api.entity.SavedJob;
import com.alumni.academic_management_api.entity.User;
import com.alumni.academic_management_api.enums.AccountStatus;
import com.alumni.academic_management_api.enums.Role;
import com.alumni.academic_management_api.repository.JobRepository;
import com.alumni.academic_management_api.repository.SavedJobRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class JobControllerIT {

    private static final String BASE_URL = "/jobs";
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
    private JobRepository jobRepository;

    @Autowired
    private SavedJobRepository savedJobRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        savedJobRepository.deleteAll();
        jobRepository.deleteAll();
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

        return loginAndGetToken(email, PASSWORD);
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequestDTO(email, password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("token").asText();
    }

    private JobRequestDTO validJobRequest() {
        return JobRequestDTO.builder()
                .title("Desenvolvedor Java")
                .company("Empresa X")
                .description("Descrição da vaga")
                .build();
    }

    private Job createJob() {
        return jobRepository.save(Job.builder()
                .title("Desenvolvedor Java")
                .company("Empresa X")
                .description("Descrição da vaga")
                .active(true)
                .build());
    }

    @Nested
    class Create {

        @Test
        void givenNoToken_whenCreate_thenReturn401() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validJobRequest())))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void givenAlumniToken_whenCreate_thenReturn403() throws Exception {
            String token = loginAndGetToken("alumni@test.com", Role.ALUMNI, "11111111111");

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validJobRequest()))
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
        }

        @Test
        void givenAdminToken_whenCreateWithValidBody_thenReturn201() throws Exception {
            String token = loginAndGetToken("admin@test.com", Role.ADMIN, "22222222222");

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validJobRequest()))
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    class Save {

        @Test
        void givenNoToken_whenSave_thenReturn401() throws Exception {
            Job job = createJob();

            mockMvc.perform(post(BASE_URL + "/" + job.getId() + "/save"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void givenAlumniToken_whenSaveExistingJob_thenReturn201() throws Exception {
            Job job = createJob();
            String token = loginAndGetToken("alumni@test.com", Role.ALUMNI, "11111111111");

            mockMvc.perform(post(BASE_URL + "/" + job.getId() + "/save")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isCreated());
        }

        @Test
        void givenAlumniToken_whenSaveAlreadySavedJob_thenReturn400() throws Exception {
            Job job = createJob();
            String token = loginAndGetToken("alumni@test.com", Role.ALUMNI, "11111111111");

            mockMvc.perform(post(BASE_URL + "/" + job.getId() + "/save")
                    .header("Authorization", "Bearer " + token));

            mockMvc.perform(post(BASE_URL + "/" + job.getId() + "/save")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void givenAlumniToken_whenSaveNonExistentJob_thenReturn404() throws Exception {
            String token = loginAndGetToken("alumni@test.com", Role.ALUMNI, "11111111111");

            mockMvc.perform(post(BASE_URL + "/9999/save")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class Unsave {

        @Test
        void givenNoToken_whenUnsave_thenReturn401() throws Exception {
            Job job = createJob();

            mockMvc.perform(delete(BASE_URL + "/" + job.getId() + "/save"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void givenAlumniToken_whenUnsaveSavedJob_thenReturn204() throws Exception {
            Job job = createJob();
            User user = userRepository.save(User.builder()
                    .name("Alumni")
                    .cpf("11111111111")
                    .email("alumni@test.com")
                    .password(passwordEncoder.encode(PASSWORD))
                    .accountStatus(AccountStatus.ACTIVE)
                    .role(Role.ALUMNI)
                    .build());
            savedJobRepository.save(SavedJob.builder().user(user).job(job).build());
            String token = loginAndGetToken(user.getEmail(), PASSWORD);

            mockMvc.perform(delete(BASE_URL + "/" + job.getId() + "/save")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNoContent());
        }

        @Test
        void givenAlumniToken_whenUnsaveJobNotSaved_thenReturn404() throws Exception {
            Job job = createJob();
            String token = loginAndGetToken("alumni@test.com", Role.ALUMNI, "11111111111");

            mockMvc.perform(delete(BASE_URL + "/" + job.getId() + "/save")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class FindSaved {

        @Test
        void givenNoToken_whenFindSaved_thenReturn401() throws Exception {
            mockMvc.perform(get(BASE_URL + "/saved"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void givenAlumniToken_whenFindSaved_thenReturn200WithSavedJobs() throws Exception {
            Job job = createJob();
            User user = userRepository.save(User.builder()
                    .name("Alumni")
                    .cpf("11111111111")
                    .email("alumni@test.com")
                    .password(passwordEncoder.encode(PASSWORD))
                    .accountStatus(AccountStatus.ACTIVE)
                    .role(Role.ALUMNI)
                    .build());
            savedJobRepository.save(SavedJob.builder().user(user).job(job).build());
            String token = loginAndGetToken(user.getEmail(), PASSWORD);

            mockMvc.perform(get(BASE_URL + "/saved")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(job.getId()));
        }
    }
}