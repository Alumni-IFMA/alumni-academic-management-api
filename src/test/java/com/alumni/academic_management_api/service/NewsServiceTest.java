package com.alumni.academic_management_api.service;

import com.alumni.academic_management_api.dto.news.NewsRequestDTO;
import com.alumni.academic_management_api.dto.news.NewsResponseDTO;
import com.alumni.academic_management_api.entity.News;
import com.alumni.academic_management_api.exception.ResourceNotFoundException;
import com.alumni.academic_management_api.mapper.NewsMapper;
import com.alumni.academic_management_api.repository.NewsRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class NewsServiceTest {

    @Mock
    private NewsRepository newsRepository;

    @Mock
    private NewsMapper newsMapper;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private NewsService newsService;

    private static final LocalDateTime PUBLISHED_AT = LocalDateTime.of(2026, 5, 16, 10, 0);

    private static News buildActiveNews() {
        return News.builder()
                .id(1L)
                .title("IFMA abre inscrições para Semana de TI")
                .summary("Evento reúne egressos e alunos ativos")
                .content("O IFMA abre inscrições para a Semana de TI 2026.")
                .coverImageUrl("https://ifma.edu.br/semana-ti.jpg")
                .publishedAt(PUBLISHED_AT)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private static NewsRequestDTO buildRequestDTO() {
        return NewsRequestDTO.builder()
                .title("IFMA abre inscrições para Semana de TI")
                .summary("Evento reúne egressos e alunos ativos")
                .content("O IFMA abre inscrições para a Semana de TI 2026.")
                .publishedAt(PUBLISHED_AT)
                .build();
    }

    private static NewsResponseDTO buildResponseDTO() {
        return NewsResponseDTO.builder()
                .id(1L)
                .title("IFMA abre inscrições para Semana de TI")
                .summary("Evento reúne egressos e alunos ativos")
                .content("O IFMA abre inscrições para a Semana de TI 2026.")
                .coverImageUrl("https://ifma.edu.br/semana-ti.jpg")
                .publishedAt(PUBLISHED_AT)
                .active(true)
                .build();
    }

    @Nested
    class Create {

        @Test
        void givenValidRequest_whenCreate_thenReturnNewsResponseDTO() {
            NewsRequestDTO request = buildRequestDTO();
            News news = buildActiveNews();
            NewsResponseDTO response = buildResponseDTO();

            Mockito.when(newsMapper.toEntity(request)).thenReturn(news);
            Mockito.when(newsRepository.save(news)).thenReturn(news);
            Mockito.when(newsMapper.toResponseDTO(news)).thenReturn(response);

            NewsResponseDTO result = newsService.create(request);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getTitle()).isEqualTo("IFMA abre inscrições para Semana de TI");
            assertThat(result.isActive()).isTrue();

            Mockito.verify(newsMapper).toEntity(request);
            Mockito.verify(newsRepository).save(news);
            Mockito.verify(newsMapper).toResponseDTO(news);
        }

        @Test
        void givenRequestWithCoverImage_whenCreate_thenUploadsFileAndSetsCoverImageUrl() {
            MockMultipartFile file = new MockMultipartFile(
                    "coverImage", "cover.jpg", "image/jpeg", new byte[]{1, 2, 3}
            );
            NewsRequestDTO request = NewsRequestDTO.builder()
                    .title("Título").content("Conteúdo").coverImage(file).build();
            News news = News.builder().title("Título").content("Conteúdo").active(true).build();
            String expectedUrl = "http://localhost:9000/alumni-files/news-images/uuid.jpg";

            Mockito.when(newsMapper.toEntity(request)).thenReturn(news);
            Mockito.when(fileStorageService.uploadFile(file, FileStorageService.FOLDER_NEWS_IMAGES))
                    .thenReturn(expectedUrl);
            Mockito.when(newsRepository.save(news)).thenReturn(news);
            Mockito.when(newsMapper.toResponseDTO(news)).thenReturn(buildResponseDTO());

            newsService.create(request);

            assertThat(news.getCoverImageUrl()).isEqualTo(expectedUrl);
            Mockito.verify(fileStorageService).uploadFile(file, FileStorageService.FOLDER_NEWS_IMAGES);
        }

        @Test
        void givenRequestWithoutCoverImage_whenCreate_thenCoverImageUrlStaysNull() {
            NewsRequestDTO request = buildRequestDTO();
            News news = News.builder().title("Título").content("Conteúdo").active(true).build();

            Mockito.when(newsMapper.toEntity(request)).thenReturn(news);
            Mockito.when(newsRepository.save(news)).thenReturn(news);
            Mockito.when(newsMapper.toResponseDTO(news)).thenReturn(buildResponseDTO());

            newsService.create(request);

            assertThat(news.getCoverImageUrl()).isNull();
            Mockito.verify(fileStorageService, Mockito.never()).uploadFile(Mockito.any(), Mockito.any());
        }
    }

    @Nested
    class FindAll {

        @Test
        void givenAdminCaller_whenFindAll_thenQueriesRepositoryIncludingDrafts() {
            News news = buildActiveNews();
            NewsResponseDTO response = buildResponseDTO();
            Pageable pageable = PageRequest.of(0, 10);
            Page<News> newsPage = new PageImpl<>(List.of(news));

            Mockito.when(newsRepository.findAllByActiveTrueOrderByPublishedAtDesc(pageable))
                    .thenReturn(newsPage);
            Mockito.when(newsMapper.toResponseDTO(news)).thenReturn(response);

            Page<NewsResponseDTO> result = newsService.findAll(pageable, true);

            assertThat(result.getTotalElements()).isEqualTo(1);
            Mockito.verify(newsRepository).findAllByActiveTrueOrderByPublishedAtDesc(pageable);
            Mockito.verify(newsRepository, Mockito.never())
                    .findAllByActiveTrueAndDraftFalseOrderByPublishedAtDesc(Mockito.any());
        }

        @Test
        void givenNonAdminCaller_whenFindAll_thenQueriesRepositoryExcludingDrafts() {
            News news = buildActiveNews();
            NewsResponseDTO response = buildResponseDTO();
            Pageable pageable = PageRequest.of(0, 10);
            Page<News> newsPage = new PageImpl<>(List.of(news));

            Mockito.when(newsRepository.findAllByActiveTrueAndDraftFalseOrderByPublishedAtDesc(pageable))
                    .thenReturn(newsPage);
            Mockito.when(newsMapper.toResponseDTO(news)).thenReturn(response);

            Page<NewsResponseDTO> result = newsService.findAll(pageable, false);

            assertThat(result.getTotalElements()).isEqualTo(1);
            Mockito.verify(newsRepository).findAllByActiveTrueAndDraftFalseOrderByPublishedAtDesc(pageable);
            Mockito.verify(newsRepository, Mockito.never())
                    .findAllByActiveTrueOrderByPublishedAtDesc(Mockito.any());
        }

        @Test
        void givenNoNews_whenFindAll_thenReturnEmptyPage() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<News> emptyPage = new PageImpl<>(List.of());

            Mockito.when(newsRepository.findAllByActiveTrueAndDraftFalseOrderByPublishedAtDesc(pageable))
                    .thenReturn(emptyPage);

            Page<NewsResponseDTO> result = newsService.findAll(pageable, false);

            assertThat(result.getTotalElements()).isEqualTo(0);
            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    class FindById {

        @Test
        void givenExistingActiveNews_whenFindById_thenReturnNewsResponseDTO() {
            News news = buildActiveNews();
            NewsResponseDTO response = buildResponseDTO();

            Mockito.when(newsRepository.findById(1L)).thenReturn(Optional.of(news));
            Mockito.when(newsMapper.toResponseDTO(news)).thenReturn(response);

            NewsResponseDTO result = newsService.findById(1L, false);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);

            Mockito.verify(newsRepository).findById(1L);
            Mockito.verify(newsMapper).toResponseDTO(news);
        }

        @Test
        void givenNonExistingNews_whenFindById_thenThrowResourceNotFoundException() {
            Mockito.when(newsRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> newsService.findById(99L, false))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");

            Mockito.verify(newsMapper, Mockito.never()).toResponseDTO(Mockito.any());
        }

        @Test
        void givenInactiveNews_whenFindById_thenThrowResourceNotFoundException() {
            News inactiveNews = News.builder()
                    .id(1L)
                    .title("Notícia Inativa")
                    .content("Conteúdo")
                    .active(false)
                    .build();

            Mockito.when(newsRepository.findById(1L)).thenReturn(Optional.of(inactiveNews));

            assertThatThrownBy(() -> newsService.findById(1L, true))
                    .isInstanceOf(ResourceNotFoundException.class);

            Mockito.verify(newsMapper, Mockito.never()).toResponseDTO(Mockito.any());
        }

        @Test
        void givenDraftNews_whenFindByIdAsAdmin_thenReturnNewsResponseDTO() {
            News draftNews = News.builder()
                    .id(2L)
                    .title("Rascunho")
                    .content("Conteúdo em rascunho")
                    .active(true)
                    .draft(true)
                    .build();
            NewsResponseDTO response = NewsResponseDTO.builder().id(2L).title("Rascunho").draft(true).build();

            Mockito.when(newsRepository.findById(2L)).thenReturn(Optional.of(draftNews));
            Mockito.when(newsMapper.toResponseDTO(draftNews)).thenReturn(response);

            NewsResponseDTO result = newsService.findById(2L, true);

            assertThat(result.isDraft()).isTrue();
        }

        @Test
        void givenDraftNews_whenFindByIdAsNonAdmin_thenThrowResourceNotFoundException() {
            News draftNews = News.builder()
                    .id(2L)
                    .title("Rascunho")
                    .content("Conteúdo em rascunho")
                    .active(true)
                    .draft(true)
                    .build();

            Mockito.when(newsRepository.findById(2L)).thenReturn(Optional.of(draftNews));

            assertThatThrownBy(() -> newsService.findById(2L, false))
                    .isInstanceOf(ResourceNotFoundException.class);

            Mockito.verify(newsMapper, Mockito.never()).toResponseDTO(Mockito.any());
        }
    }

    @Nested
    class Update {

        @Test
        void givenValidRequest_whenUpdate_thenReturnUpdatedNewsResponseDTO() {
            News news = buildActiveNews();
            NewsRequestDTO request = buildRequestDTO();
            NewsResponseDTO response = buildResponseDTO();

            Mockito.when(newsRepository.findById(1L)).thenReturn(Optional.of(news));
            Mockito.doNothing().when(newsMapper).updateEntityFromDTO(request, news);
            Mockito.when(newsRepository.save(news)).thenReturn(news);
            Mockito.when(newsMapper.toResponseDTO(news)).thenReturn(response);

            NewsResponseDTO result = newsService.update(1L, request);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);

            Mockito.verify(newsMapper).updateEntityFromDTO(request, news);
            Mockito.verify(newsRepository).save(news);
        }

        @Test
        void givenNonExistingNews_whenUpdate_thenThrowResourceNotFoundException() {
            Mockito.when(newsRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> newsService.update(99L, buildRequestDTO()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");

            Mockito.verify(newsRepository, Mockito.never()).save(Mockito.any());
        }

        @Test
        void givenInactiveNews_whenUpdate_thenThrowResourceNotFoundException() {
            News inactiveNews = News.builder()
                    .id(1L)
                    .title("Notícia Inativa")
                    .content("Conteúdo")
                    .active(false)
                    .build();

            Mockito.when(newsRepository.findById(1L)).thenReturn(Optional.of(inactiveNews));

            assertThatThrownBy(() -> newsService.update(1L, buildRequestDTO()))
                    .isInstanceOf(ResourceNotFoundException.class);

            Mockito.verify(newsRepository, Mockito.never()).save(Mockito.any());
        }

        @Test
        void givenNewCoverImage_whenUpdate_thenDeletesOldFileAndUploadsNew() {
            String oldUrl = "http://localhost:9000/alumni-files/news-images/old.jpg";
            News news = News.builder().id(1L).title("Título").content("Conteúdo")
                    .active(true).coverImageUrl(oldUrl).build();
            MockMultipartFile file = new MockMultipartFile(
                    "coverImage", "new.jpg", "image/jpeg", new byte[]{1}
            );
            NewsRequestDTO request = NewsRequestDTO.builder()
                    .title("Título").content("Conteúdo").coverImage(file).build();
            String newUrl = "http://localhost:9000/alumni-files/news-images/new-uuid.jpg";

            Mockito.when(newsRepository.findById(1L)).thenReturn(Optional.of(news));
            Mockito.when(fileStorageService.uploadFile(file, FileStorageService.FOLDER_NEWS_IMAGES))
                    .thenReturn(newUrl);
            Mockito.when(newsRepository.save(news)).thenReturn(news);
            Mockito.when(newsMapper.toResponseDTO(news)).thenReturn(buildResponseDTO());

            newsService.update(1L, request);

            Mockito.verify(fileStorageService).deleteFile(oldUrl);
            assertThat(news.getCoverImageUrl()).isEqualTo(newUrl);
        }

        @Test
        void givenNoCoverImage_whenUpdate_thenKeepsExistingCoverImageUrl() {
            String existingUrl = "http://localhost:9000/alumni-files/news-images/existing.jpg";
            News news = News.builder().id(1L).title("Título").content("Conteúdo")
                    .active(true).coverImageUrl(existingUrl).build();
            NewsRequestDTO request = NewsRequestDTO.builder().title("Título").content("Conteúdo").build();

            Mockito.when(newsRepository.findById(1L)).thenReturn(Optional.of(news));
            Mockito.when(newsRepository.save(news)).thenReturn(news);
            Mockito.when(newsMapper.toResponseDTO(news)).thenReturn(buildResponseDTO());

            newsService.update(1L, request);

            assertThat(news.getCoverImageUrl()).isEqualTo(existingUrl);
            Mockito.verify(fileStorageService, Mockito.never()).uploadFile(Mockito.any(), Mockito.any());
            Mockito.verify(fileStorageService, Mockito.never()).deleteFile(Mockito.any());
        }
    }

    @Nested
    class Delete {

        @Test
        void givenExistingNews_whenDelete_thenSetActiveToFalse() {
            News news = buildActiveNews();

            Mockito.when(newsRepository.findById(1L)).thenReturn(Optional.of(news));

            newsService.delete(1L);

            assertThat(news.isActive()).isFalse();
            Mockito.verify(newsRepository).save(news);
        }

        @Test
        void givenNonExistingNews_whenDelete_thenThrowResourceNotFoundException() {
            Mockito.when(newsRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> newsService.delete(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");

            Mockito.verify(newsRepository, Mockito.never()).save(Mockito.any());
        }

        @Test
        void givenInactiveNews_whenDelete_thenThrowResourceNotFoundException() {
            News inactiveNews = News.builder()
                    .id(1L)
                    .title("Notícia Inativa")
                    .content("Conteúdo")
                    .active(false)
                    .build();

            Mockito.when(newsRepository.findById(1L)).thenReturn(Optional.of(inactiveNews));

            assertThatThrownBy(() -> newsService.delete(1L))
                    .isInstanceOf(ResourceNotFoundException.class);

            Mockito.verify(newsRepository, Mockito.never()).save(Mockito.any());
        }
    }
}
