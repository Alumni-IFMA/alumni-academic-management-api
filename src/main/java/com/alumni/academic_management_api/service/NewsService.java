package com.alumni.academic_management_api.service;

import com.alumni.academic_management_api.dto.news.NewsRequestDTO;
import com.alumni.academic_management_api.dto.news.NewsResponseDTO;
import com.alumni.academic_management_api.entity.News;
import com.alumni.academic_management_api.exception.ResourceNotFoundException;
import com.alumni.academic_management_api.mapper.NewsMapper;
import com.alumni.academic_management_api.repository.NewsRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Service
public class NewsService {

    private static final int COVER_IMAGE_URL_EXPIRY_MINUTES = 1440;

    private final NewsRepository newsRepository;
    private final NewsMapper newsMapper;
    private final FileStorageService fileStorageService;

    public NewsService(NewsRepository newsRepository,
                       NewsMapper newsMapper,
                       FileStorageService fileStorageService) {
        this.newsRepository = newsRepository;
        this.newsMapper = newsMapper;
        this.fileStorageService = fileStorageService;
    }

    public NewsResponseDTO create(NewsRequestDTO dto) {
        News news = newsMapper.toEntity(dto);
        news.setActive(true);
        applyCoverImage(dto, news);
        return buildResponse(newsRepository.save(news));
    }

    @Transactional(readOnly = true)
    public Page<NewsResponseDTO> findAll(Pageable pageable, boolean isAdmin) {
        Page<News> page = isAdmin
                ? newsRepository.findAllByActiveTrueOrderByPublishedAtDesc(pageable)
                : newsRepository.findAllByActiveTrueAndDraftFalseOrderByPublishedAtDesc(pageable);
        return page.map(this::buildResponse);
    }

    @Transactional(readOnly = true)
    public NewsResponseDTO findById(Long id, boolean isAdmin) {
        News news = newsRepository.findById(id)
                .filter(News::isActive)
                .filter(n -> isAdmin || !n.isDraft())
                .orElseThrow(() -> new ResourceNotFoundException("News not found with id: " + id));
        return buildResponse(news);
    }

    public NewsResponseDTO update(Long id, NewsRequestDTO dto) {
        News news = newsRepository.findById(id)
                .filter(News::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("News not found with id: " + id));
        newsMapper.updateEntityFromDTO(dto, news);
        applyCoverImage(dto, news);
        return buildResponse(newsRepository.save(news));
    }

    public void delete(Long id) {
        News news = newsRepository.findById(id)
                .filter(News::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("News not found with id: " + id));
        news.setActive(false);
        newsRepository.save(news);
    }

    private void applyCoverImage(NewsRequestDTO dto, News news) {
        if (dto.getCoverImage() == null || dto.getCoverImage().isEmpty()) {
            return;
        }
        if (news.getCoverImageUrl() != null) {
            fileStorageService.deleteFile(news.getCoverImageUrl());
        }
        String url = fileStorageService.uploadFile(dto.getCoverImage(), FileStorageService.FOLDER_NEWS_IMAGES);
        news.setCoverImageUrl(url);
    }

    private NewsResponseDTO buildResponse(News news) {
        NewsResponseDTO dto = newsMapper.toResponseDTO(news);
        if (dto.getCoverImageUrl() != null) {
            dto.setCoverImageUrl(
                    fileStorageService.generatePresignedUrl(dto.getCoverImageUrl(), COVER_IMAGE_URL_EXPIRY_MINUTES));
        }
        return dto;
    }
}
