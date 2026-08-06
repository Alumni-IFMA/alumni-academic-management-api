package com.alumni.academic_management_api.controller;

import com.alumni.academic_management_api.dto.auth.LoginRequestDTO;
import com.alumni.academic_management_api.entity.News;
import com.alumni.academic_management_api.entity.User;
import com.alumni.academic_management_api.enums.AccountStatus;
import com.alumni.academic_management_api.enums.Role;
import com.alumni.academic_management_api.repository.NewsRepository;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class NewsControllerIT {

    private static final String BASE_URL = "/news";
    private static final String PASSWORD = "senha12345";
    private static final LocalDateTime PUBLISHED_AT = LocalDateTime.of(2026, 5, 16, 10, 0);

    @MockBean
    private MinioClient minioClient;

    @MockBean
    private FileStorageService fileStorageService;

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

    private News saveDraftNews(String title) {
        return newsRepository.save(News.builder()
                .title(title)
                .summary("Um resumo de teste")
                .content("Conteúdo completo da notícia de teste.")
                .publishedAt(PUBLISHED_AT)
                .active(true)
                .draft(true)
                .build());
    }

    @Nested
    class Create {

        @Test
        void givenNoToken_whenCreate_thenReturn401() throws Exception {
            mockMvc.perform(multipart(BASE_URL)
                            .param("title", "Nova Notícia")
                            .param("content", "Conteúdo da notícia."))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void givenAlumniToken_whenCreate_thenReturn403() throws Exception {
            String token = loginAndGetToken("alumni@test.com", Role.ALUMNI, "11111111111");

            mockMvc.perform(multipart(BASE_URL)
                            .param("title", "Nova Notícia")
                            .param("content", "Conteúdo da notícia.")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
        }

        @Test
        void givenAdminToken_whenCreateWithValidBody_thenReturn201() throws Exception {
            String token = loginAndGetToken("admin@test.com", Role.ADMIN, "22222222222");

            mockMvc.perform(multipart(BASE_URL)
                            .param("title", "IFMA abre inscrições para Semana de TI")
                            .param("summary", "Evento reúne egressos e alunos ativos")
                            .param("content", "O IFMA abre inscrições para a Semana de TI 2026.")
                            .param("publishedAt", PUBLISHED_AT.toString())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.title").value("IFMA abre inscrições para Semana de TI"))
                    .andExpect(jsonPath("$.active").value(true))
                    .andExpect(jsonPath("$.draft").value(false));
        }

        @Test
        void givenAdminToken_whenCreateWithBlankTitle_thenReturn400() throws Exception {
            String token = loginAndGetToken("admin@test.com", Role.ADMIN, "33333333333");

            mockMvc.perform(multipart(BASE_URL)
                            .param("title", "")
                            .param("content", "Conteúdo da notícia.")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void givenAdminTokenWithCoverImage_whenCreate_thenReturn201WithPresignedCoverImageUrl() throws Exception {
            String token = loginAndGetToken("admin@test.com", Role.ADMIN, "10101010101");
            String rawUrl = "http://localhost:9000/alumni-files/news-images/uuid.jpg";
            String presignedUrl = rawUrl + "?X-Amz-Signature=test";
            when(fileStorageService.uploadFile(any(), any())).thenReturn(rawUrl);
            when(fileStorageService.generatePresignedUrl(eq(rawUrl), anyInt())).thenReturn(presignedUrl);
            MockMultipartFile file = new MockMultipartFile(
                    "coverImage", "cover.jpg", "image/jpeg", new byte[]{1, 2, 3}
            );

            mockMvc.perform(multipart(BASE_URL)
                            .file(file)
                            .param("title", "Notícia com Capa")
                            .param("content", "Conteúdo")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.coverImageUrl").value(presignedUrl));
        }

        @Test
        void givenAdminTokenWithDraftTrue_whenCreate_thenReturnDraftTrue() throws Exception {
            String token = loginAndGetToken("admin@test.com", Role.ADMIN, "12121212121");

            mockMvc.perform(multipart(BASE_URL)
                            .param("title", "Rascunho")
                            .param("content", "Conteúdo em rascunho")
                            .param("draft", "true")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.draft").value(true));
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

        @Test
        void givenDraftNews_whenFindAllAsAnonymous_thenExcludedFromContent() throws Exception {
            saveDraftNews("Rascunho");

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        void givenDraftNews_whenFindAllAsAlumni_thenExcludedFromContent() throws Exception {
            String token = loginAndGetToken("alumni2@test.com", Role.ALUMNI, "13131313131");
            saveDraftNews("Rascunho");

            mockMvc.perform(get(BASE_URL).header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        void givenDraftNews_whenFindAllAsAdmin_thenIncludedInContent() throws Exception {
            String token = loginAndGetToken("admin4@test.com", Role.ADMIN, "14141414141");
            saveDraftNews("Rascunho");

            mockMvc.perform(get(BASE_URL).header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].draft").value(true));
        }

        @Test
        void givenNewsWithCoverImage_whenFindAll_thenReturnsPresignedCoverImageUrl() throws Exception {
            News news = saveActiveNews("Notícia com Capa");
            String presignedUrl = news.getCoverImageUrl() + "?X-Amz-Signature=test";
            when(fileStorageService.generatePresignedUrl(eq(news.getCoverImageUrl()), anyInt()))
                    .thenReturn(presignedUrl);

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].coverImageUrl").value(presignedUrl));
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

        @Test
        void givenDraftNews_whenFindByIdAsAnonymous_thenReturn404() throws Exception {
            News draft = saveDraftNews("Rascunho");

            mockMvc.perform(get(BASE_URL + "/{id}", draft.getId()))
                    .andExpect(status().isNotFound());
        }

        @Test
        void givenDraftNews_whenFindByIdAsAdmin_thenReturn200() throws Exception {
            String token = loginAndGetToken("admin5@test.com", Role.ADMIN, "15151515151");
            News draft = saveDraftNews("Rascunho");

            mockMvc.perform(get(BASE_URL + "/{id}", draft.getId())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.draft").value(true));
        }

        @Test
        void givenNewsWithCoverImage_whenFindById_thenReturnsPresignedCoverImageUrl() throws Exception {
            News news = saveActiveNews("Notícia com Capa");
            String presignedUrl = news.getCoverImageUrl() + "?X-Amz-Signature=test";
            when(fileStorageService.generatePresignedUrl(eq(news.getCoverImageUrl()), anyInt()))
                    .thenReturn(presignedUrl);

            mockMvc.perform(get(BASE_URL + "/{id}", news.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.coverImageUrl").value(presignedUrl));
        }
    }

    @Nested
    class Update {

        @Test
        void givenNoToken_whenUpdate_thenReturn401() throws Exception {
            News news = saveActiveNews("Notícia Original");

            mockMvc.perform(multipart(HttpMethod.PUT, BASE_URL + "/{id}", news.getId())
                            .param("title", "Título Atualizado")
                            .param("content", "Conteúdo atualizado."))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void givenAlumniToken_whenUpdate_thenReturn403() throws Exception {
            String token = loginAndGetToken("alumni@test.com", Role.ALUMNI, "44444444444");
            News news = saveActiveNews("Notícia Original");

            mockMvc.perform(multipart(HttpMethod.PUT, BASE_URL + "/{id}", news.getId())
                            .param("title", "Título Atualizado")
                            .param("content", "Conteúdo atualizado.")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
        }

        @Test
        void givenAdminToken_whenUpdateExistingNews_thenReturn200() throws Exception {
            String token = loginAndGetToken("admin@test.com", Role.ADMIN, "55555555555");
            News news = saveActiveNews("Notícia Original");

            mockMvc.perform(multipart(HttpMethod.PUT, BASE_URL + "/{id}", news.getId())
                            .param("title", "Título Atualizado")
                            .param("content", "Conteúdo atualizado.")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Título Atualizado"));
        }

        @Test
        void givenAdminToken_whenUpdateNonExistingNews_thenReturn404() throws Exception {
            String token = loginAndGetToken("admin@test.com", Role.ADMIN, "66666666666");

            mockMvc.perform(multipart(HttpMethod.PUT, BASE_URL + "/{id}", 999999L)
                            .param("title", "Título Atualizado")
                            .param("content", "Conteúdo atualizado.")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isNotFound());
        }

        @Test
        void givenAdminTokenWithNewCoverImage_whenUpdate_thenReplacesCoverImageUrlWithPresignedOne() throws Exception {
            String token = loginAndGetToken("admin6@test.com", Role.ADMIN, "16161616161");
            News news = saveActiveNews("Notícia com Capa");
            String newUrl = "http://localhost:9000/alumni-files/news-images/new-uuid.jpg";
            String presignedUrl = newUrl + "?X-Amz-Signature=test";
            when(fileStorageService.uploadFile(any(), any())).thenReturn(newUrl);
            when(fileStorageService.generatePresignedUrl(eq(newUrl), anyInt())).thenReturn(presignedUrl);
            MockMultipartFile file = new MockMultipartFile(
                    "coverImage", "new.jpg", "image/jpeg", new byte[]{1, 2, 3}
            );

            mockMvc.perform(multipart(HttpMethod.PUT, BASE_URL + "/{id}", news.getId())
                            .file(file)
                            .param("title", news.getTitle())
                            .param("content", news.getContent())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.coverImageUrl").value(presignedUrl));
        }

        @Test
        void givenAdminTokenWithoutCoverImage_whenUpdate_thenKeepsPresignedExistingCoverImageUrl() throws Exception {
            String token = loginAndGetToken("admin7@test.com", Role.ADMIN, "17171717171");
            News news = saveActiveNews("Notícia com Capa");
            String presignedUrl = news.getCoverImageUrl() + "?X-Amz-Signature=test";
            when(fileStorageService.generatePresignedUrl(eq(news.getCoverImageUrl()), anyInt()))
                    .thenReturn(presignedUrl);

            mockMvc.perform(multipart(HttpMethod.PUT, BASE_URL + "/{id}", news.getId())
                            .param("title", "Título Atualizado")
                            .param("content", news.getContent())
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.coverImageUrl").value(presignedUrl));
        }
    }
}
