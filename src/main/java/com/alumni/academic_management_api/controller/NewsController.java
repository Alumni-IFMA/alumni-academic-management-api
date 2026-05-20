package com.alumni.academic_management_api.controller;

import com.alumni.academic_management_api.dto.news.NewsRequestDTO;
import com.alumni.academic_management_api.dto.news.NewsResponseDTO;
import com.alumni.academic_management_api.service.NewsService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/news")
public class NewsController {

    private final NewsService newsService;

    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    @PostMapping
    public ResponseEntity<NewsResponseDTO> create(@RequestBody @Valid NewsRequestDTO request) {
        log.debug("REST request to create news");
        return ResponseEntity.status(HttpStatus.CREATED).body(newsService.create(request));
    }

    @GetMapping
    public ResponseEntity<Page<NewsResponseDTO>> findAll(Pageable pageable) {
        log.debug("REST request to list news");
        return ResponseEntity.ok(newsService.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NewsResponseDTO> findById(@PathVariable Long id) {
        log.debug("REST request to get news: {}", id);
        return ResponseEntity.ok(newsService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<NewsResponseDTO> update(
            @PathVariable Long id,
            @RequestBody @Valid NewsRequestDTO request
    ) {
        log.debug("REST request to update news: {}", id);
        return ResponseEntity.ok(newsService.update(id, request));
    }
}
