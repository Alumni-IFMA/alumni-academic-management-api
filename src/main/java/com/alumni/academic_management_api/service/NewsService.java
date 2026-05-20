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

    private final NewsRepository newsRepository;
    private final NewsMapper newsMapper;

    public NewsService(NewsRepository newsRepository, NewsMapper newsMapper) {
        this.newsRepository = newsRepository;
        this.newsMapper = newsMapper;
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
}
