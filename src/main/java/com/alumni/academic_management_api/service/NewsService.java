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
import org.springframework.web.multipart.MultipartFile;

@Transactional
@Service
public class NewsService {

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
        return newsMapper.toResponseDTO(newsRepository.save(news));
    }

    @Transactional(readOnly = true)
    public Page<NewsResponseDTO> findAll(Pageable pageable) {
        return newsRepository.findAllByActiveTrueOrderByPublishedAtDesc(pageable)
                .map(newsMapper::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public NewsResponseDTO findById(Long id) {
        News news = newsRepository.findById(id)
                .filter(News::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("News not found with id: " + id));
        return newsMapper.toResponseDTO(news);
    }

    public NewsResponseDTO update(Long id, NewsRequestDTO dto) {
        News news = newsRepository.findById(id)
                .filter(News::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("News not found with id: " + id));
        newsMapper.updateEntityFromDTO(dto, news);
        return newsMapper.toResponseDTO(newsRepository.save(news));
    }

    public String uploadCoverImage(Long newsId, MultipartFile file) {
        News news = newsRepository.findById(newsId)
                .filter(News::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("News not found with id: " + newsId));
        if (news.getCoverImageUrl() != null) {
            fileStorageService.deleteFile(news.getCoverImageUrl());
        }
        String url = fileStorageService.uploadFile(file, FileStorageService.FOLDER_NEWS_IMAGES);
        news.setCoverImageUrl(url);
        newsRepository.save(news);
        return url;
    }

    public void delete(Long id) {
        News news = newsRepository.findById(id)
                .filter(News::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("News not found with id: " + id));
        news.setActive(false);
        newsRepository.save(news);
    }
}
