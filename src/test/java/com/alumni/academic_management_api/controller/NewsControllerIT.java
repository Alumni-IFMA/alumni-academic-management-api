package com.alumni.academic_management_api.controller;

import com.alumni.academic_management_api.dto.auth.LoginRequestDTO;
import com.alumni.academic_management_api.dto.news.NewsRequestDTO;
import com.alumni.academic_management_api.entity.News;
import com.alumni.academic_management_api.entity.User;
import com.alumni.academic_management_api.enums.AccountStatus;
import com.alumni.academic_management_api.enums.Role;
import com.alumni.academic_management_api.repository.NewsRepository;
import com.alumni.academic_management_api.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class NewsControllerIT {

    private static final String BASE_URL = "/news";
    private static final String PASSWORD = "senha12345";
    private static final LocalDateTime PUBLISHED_AT = LocalDateTime.of(2026, 5, 16, 10, 0);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NewsRepository newsRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        newsRepository.deleteAll();
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

    private News saveActiveNews(String title) {
        return newsRepository.save(News.builder()
                .title(title)
                .summary("Um resumo de teste")
                .content("Conteúdo completo da notícia de teste.")
                .coverImageUrl("https://ifma.edu.br/cover.jpg")
                .publishedAt(PUBLISHED_AT)
                .active(true)
                .build());
    }

    @Nested
    class Create {

        @Test
        void givenNoToken_whenCreate_thenReturn401() throws Exception {
            NewsRequestDTO request = NewsRequestDTO.builder()
                    .title("Nova Notícia")
                    .content("Conteúdo da notícia.")
                    .build();

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void givenAlumniToken_whenCreate_thenReturn403() throws Exception {
            String token = loginAndGetToken("alumni@test.com", Role.ALUMNI, "11111111111");
            NewsRequestDTO request = NewsRequestDTO.builder()
                    .title("Nova Notícia")
                    .content("Conteúdo da notícia.")
                    .build();

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
        }

        @Test
        void givenAdminToken_whenCreateWithValidBody_thenReturn201() throws Exception {
            String token = loginAndGetToken("admin@test.com", Role.ADMIN, "22222222222");
            NewsRequestDTO request = NewsRequestDTO.builder()
                    .title("IFMA abre inscrições para Semana de TI")
                    .summary("Evento reúne egressos e alunos ativos")
                    .content("O IFMA abre inscrições para a Semana de TI 2026.")
                    .coverImageUrl("https://ifma.edu.br/semana-ti.jpg")
                    .publishedAt(PUBLISHED_AT)
                    .build();

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.title").value("IFMA abre inscrições para Semana de TI"))
                    .andExpect(jsonPath("$.active").value(true));
        }

        @Test
        void givenAdminToken_whenCreateWithBlankTitle_thenReturn400() throws Exception {
            String token = loginAndGetToken("admin@test.com", Role.ADMIN, "33333333333");
            NewsRequestDTO request = NewsRequestDTO.builder()
                    .title("")
                    .content("Conteúdo da notícia.")
                    .build();

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class FindAll {

        @Test
        void givenNoToken_whenFindAll_thenReturn200() throws Exception {
            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk());
        }

        @Test
        void givenActiveNews_whenFindAll_thenReturnNewsInContent() throws Exception {
            saveActiveNews("Notícia de Teste");

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0].title").value("Notícia de Teste"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        void givenOnlyInactiveNews_whenFindAll_thenReturnEmptyContent() throws Exception {
            newsRepository.save(News.builder()
                    .title("Notícia Inativa")
                    .content("Conteúdo")
                    .active(false)
                    .build());

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.totalElements").value(0));
        }
    }

    @Nested
    class FindById {

        @Test
        void givenExistingActiveNews_whenFindById_thenReturn200() throws Exception {
            News news = saveActiveNews("Notícia de Teste");

            mockMvc.perform(get(BASE_URL + "/{id}", news.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(news.getId()))
                    .andExpect(jsonPath("$.title").value("Notícia de Teste"));
        }

        @Test
        void givenNonExistingNews_whenFindById_thenReturn404() throws Exception {
            mockMvc.perform(get(BASE_URL + "/{id}", 999999L))
                    .andExpect(status().isNotFound());
        }

        @Test
        void givenInactiveNews_whenFindById_thenReturn404() throws Exception {
            News inactiveNews = newsRepository.save(News.builder()
                    .title("Notícia Inativa")
                    .content("Conteúdo")
                    .active(false)
                    .build());

            mockMvc.perform(get(BASE_URL + "/{id}", inactiveNews.getId()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class Update {

        @Test
        void givenNoToken_whenUpdate_thenReturn401() throws Exception {
            News news = saveActiveNews("Notícia Original");
            NewsRequestDTO request = NewsRequestDTO.builder()
                    .title("Título Atualizado")
                    .content("Conteúdo atualizado.")
                    .build();

            mockMvc.perform(put(BASE_URL + "/{id}", news.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void givenAlumniToken_whenUpdate_thenReturn403() throws Exception {
            String token = loginAndGetToken("alumni@test.com", Role.ALUMNI, "44444444444");
            News news = saveActiveNews("Notícia Original");
            NewsRequestDTO request = NewsRequestDTO.builder()
                    .title("Título Atualizado")
                    .content("Conteúdo atualizado.")
                    .build();

            mockMvc.perform(put(BASE_URL + "/{id}", news.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
        }

        @Test
        void givenAdminToken_whenUpdateExistingNews_thenReturn200() throws Exception {
            String token = loginAndGetToken("admin@test.com", Role.ADMIN, "55555555555");
            News news = saveActiveNews("Notícia Original");
            NewsRequestDTO request = NewsRequestDTO.builder()
                    .title("Título Atualizado")
                    .content("Conteúdo atualizado.")
                    .build();

            mockMvc.perform(put(BASE_URL + "/{id}", news.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Título Atualizado"));
        }

        @Test
        void givenAdminToken_whenUpdateNonExistingNews_thenReturn404() throws Exception {
            String token = loginAndGetToken("admin@test.com", Role.ADMIN, "66666666666");
            NewsRequestDTO request = NewsRequestDTO.builder()
                    .title("Título Atualizado")
                    .content("Conteúdo atualizado.")
                    .build();

            mockMvc.perform(put(BASE_URL + "/{id}", 999999L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound());
        }
    }
}
