package com.alumni.academic_management_api.controller;

import com.alumni.academic_management_api.dto.support.SupportMessageRequestDTO;
import com.alumni.academic_management_api.entity.SupportMessage;
import com.alumni.academic_management_api.entity.User;
import com.alumni.academic_management_api.enums.AccountStatus;
import com.alumni.academic_management_api.enums.Role;
import com.alumni.academic_management_api.repository.SupportMessageRepository;
import com.alumni.academic_management_api.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SupportMessageControllerIT {

    private static final String USER_EMAIL = "joao.support@email.com";

    @MockBean
    private MinioClient minioClient;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SupportMessageRepository supportMessageRepository;

    @BeforeEach
    void setUp() {
        supportMessageRepository.deleteAll();
        userRepository.deleteAll();
    }

    private User saveUser(String email, String cpf) {
        return userRepository.save(User.builder()
                .name("João Silva")
                .cpf(cpf)
                .email(email)
                .accountStatus(AccountStatus.ACTIVE)
                .role(Role.ALUMNI)
                .build());
    }

    @Nested
    class Send {

        private static final String URL = "/support";

        @Test
        @WithMockUser(username = USER_EMAIL)
        void givenValidRequest_whenSend_thenReturn201() throws Exception {
            saveUser(USER_EMAIL, "11111111111");

            SupportMessageRequestDTO request = SupportMessageRequestDTO.builder()
                    .subject("Dúvida sobre certificado")
                    .message("Como emito meu certificado?")
                    .build();

            mockMvc.perform(post(URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("João Silva"))
                    .andExpect(jsonPath("$.email").value(USER_EMAIL))
                    .andExpect(jsonPath("$.resolved").value(false));
        }

        @Test
        @WithMockUser(username = USER_EMAIL)
        void givenBlankSubject_whenSend_thenReturn400() throws Exception {
            saveUser(USER_EMAIL, "22222222222");

            SupportMessageRequestDTO request = SupportMessageRequestDTO.builder()
                    .subject("")
                    .message("Mensagem")
                    .build();

            mockMvc.perform(post(URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void givenNoToken_whenSend_thenReturn401() throws Exception {
            SupportMessageRequestDTO request = SupportMessageRequestDTO.builder()
                    .subject("Assunto")
                    .message("Mensagem")
                    .build();

            mockMvc.perform(post(URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class FindAll {

        private static final String URL = "/support";

        @Test
        @WithMockUser(roles = "ADMIN")
        void givenAdmin_whenFindAll_thenReturn200WithMessages() throws Exception {
            User user = saveUser(USER_EMAIL, "33333333333");
            supportMessageRepository.save(SupportMessage.builder()
                    .user(user).name(user.getName()).email(user.getEmail())
                    .subject("Assunto 1").message("Mensagem 1").build());

            mockMvc.perform(get(URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void givenResolvedFilter_whenFindAll_thenReturnOnlyResolved() throws Exception {
            User user = saveUser(USER_EMAIL, "44444444444");
            supportMessageRepository.save(SupportMessage.builder()
                    .user(user).name(user.getName()).email(user.getEmail())
                    .subject("Não resolvida").message("Mensagem").resolved(false).build());
            supportMessageRepository.save(SupportMessage.builder()
                    .user(user).name(user.getName()).email(user.getEmail())
                    .subject("Resolvida").message("Mensagem").resolved(true).build());

            mockMvc.perform(get(URL).param("resolved", "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].subject").value("Resolvida"));
        }

        @Test
        @WithMockUser(roles = "ALUMNI")
        void givenNonAdmin_whenFindAll_thenReturn403() throws Exception {
            mockMvc.perform(get(URL))
                    .andExpect(status().isForbidden());
        }

        @Test
        void givenNoToken_whenFindAll_thenReturn401() throws Exception {
            mockMvc.perform(get(URL))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class Resolve {

        private String url(Long id) {
            return "/support/" + id + "/resolve";
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void givenValidId_whenResolve_thenReturn200WithResolvedTrue() throws Exception {
            User user = saveUser(USER_EMAIL, "55555555555");
            SupportMessage message = supportMessageRepository.save(SupportMessage.builder()
                    .user(user).name(user.getName()).email(user.getEmail())
                    .subject("Assunto").message("Mensagem").build());

            mockMvc.perform(patch(url(message.getId())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resolved").value(true));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void givenInvalidId_whenResolve_thenReturn404() throws Exception {
            mockMvc.perform(patch(url(9999L)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(roles = "ALUMNI")
        void givenNonAdmin_whenResolve_thenReturn403() throws Exception {
            mockMvc.perform(patch(url(1L)))
                    .andExpect(status().isForbidden());
        }
    }
}
