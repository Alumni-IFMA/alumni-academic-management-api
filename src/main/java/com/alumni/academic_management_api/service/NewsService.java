package com.alumni.academic_management_api.service;

import com.alumni.academic_management_api.dto.news.NewsRequestDTO;
import com.alumni.academic_management_api.dto.news.NewsResponseDTO;
import com.alumni.academic_management_api.entity.News;
import com.alumni.academic_management_api.mapper.NewsMapper;
import com.alumni.academic_management_api.repository.NewsRepository;
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
}
