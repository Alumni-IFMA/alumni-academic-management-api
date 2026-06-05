package com.alumni.academic_management_api.service;

import com.alumni.academic_management_api.dto.news.NewsRequestDTO;
import com.alumni.academic_management_api.dto.news.NewsResponseDTO;
import com.alumni.academic_management_api.entity.News;
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

import com.alumni.academic_management_api.exception.ResourceNotFoundException;

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
                .coverImageUrl("https://ifma.edu.br/semana-ti.jpg")
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
    }

    @Nested
    class FindAll {

        @Test
        void givenExistingNews_whenFindAll_thenReturnPage() {
            News news = buildActiveNews();
            NewsResponseDTO response = buildResponseDTO();
            Pageable pageable = PageRequest.of(0, 10);
            Page<News> newsPage = new PageImpl<>(List.of(news));

            Mockito.when(newsRepository.findAllByActiveTrueOrderByPublishedAtDesc(pageable))
                    .thenReturn(newsPage);
            Mockito.when(newsMapper.toResponseDTO(news)).thenReturn(response);

            Page<NewsResponseDTO> result = newsService.findAll(pageable);

            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getTitle())
                    .isEqualTo("IFMA abre inscrições para Semana de TI");
        }

        @Test
        void givenNoNews_whenFindAll_thenReturnEmptyPage() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<News> emptyPage = new PageImpl<>(List.of());

            Mockito.when(newsRepository.findAllByActiveTrueOrderByPublishedAtDesc(pageable))
                    .thenReturn(emptyPage);

            Page<NewsResponseDTO> result = newsService.findAll(pageable);

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

            NewsResponseDTO result = newsService.findById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);

            Mockito.verify(newsRepository).findById(1L);
            Mockito.verify(newsMapper).toResponseDTO(news);
        }

        @Test
        void givenNonExistingNews_whenFindById_thenThrowResourceNotFoundException() {
            Mockito.when(newsRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> newsService.findById(99L))
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

            assertThatThrownBy(() -> newsService.findById(1L))
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

    @Nested
    class UploadCoverImage {

        @Test
        void givenExistingNewsWithNoCover_whenUploadCoverImage_thenReturnNewUrl() {
            Long newsId = 1L;
            News news = News.builder().id(newsId).active(true).build();
            MockMultipartFile file = new MockMultipartFile(
                    "file", "cover.jpg", "image/jpeg", new byte[]{1, 2, 3}
            );
            String expectedUrl = "http://localhost:9000/alumni-files/news-images/uuid.jpg";
            Mockito.when(newsRepository.findById(newsId)).thenReturn(Optional.of(news));
            Mockito.when(fileStorageService.uploadFile(file, FileStorageService.FOLDER_NEWS_IMAGES))
                    .thenReturn(expectedUrl);

            String result = newsService.uploadCoverImage(newsId, file);

            assertThat(result).isEqualTo(expectedUrl);
            assertThat(news.getCoverImageUrl()).isEqualTo(expectedUrl);
            Mockito.verify(newsRepository).save(news);
        }

        @Test
        void givenNewsWithExistingCover_whenUploadCoverImage_thenDeleteOldBeforeUploadingNew() {
            Long newsId = 1L;
            String oldUrl = "http://localhost:9000/alumni-files/news-images/old.jpg";
            News news = News.builder().id(newsId).active(true).coverImageUrl(oldUrl).build();
            MockMultipartFile file = new MockMultipartFile(
                    "file", "new.jpg", "image/jpeg", new byte[]{1}
            );
            Mockito.when(newsRepository.findById(newsId)).thenReturn(Optional.of(news));
            Mockito.when(fileStorageService.uploadFile(file, FileStorageService.FOLDER_NEWS_IMAGES))
                    .thenReturn("http://localhost:9000/alumni-files/news-images/new-uuid.jpg");

            newsService.uploadCoverImage(newsId, file);

            Mockito.verify(fileStorageService).deleteFile(oldUrl);
        }

        @Test
        void givenNonExistentNewsId_whenUploadCoverImage_thenThrowResourceNotFoundException() {
            Long newsId = 999L;
            MockMultipartFile file = new MockMultipartFile(
                    "file", "cover.jpg", "image/jpeg", new byte[]{1}
            );
            Mockito.when(newsRepository.findById(newsId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> newsService.uploadCoverImage(newsId, file))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("999");
        }

        @Test
        void givenInactiveNews_whenUploadCoverImage_thenThrowResourceNotFoundException() {
            Long newsId = 1L;
            News inactiveNews = News.builder().id(newsId).active(false).build();
            MockMultipartFile file = new MockMultipartFile(
                    "file", "cover.jpg", "image/jpeg", new byte[]{1}
            );
            Mockito.when(newsRepository.findById(newsId)).thenReturn(Optional.of(inactiveNews));

            assertThatThrownBy(() -> newsService.uploadCoverImage(newsId, file))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
