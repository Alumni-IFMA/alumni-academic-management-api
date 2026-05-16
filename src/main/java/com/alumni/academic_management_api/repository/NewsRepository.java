package com.alumni.academic_management_api.repository;

import com.alumni.academic_management_api.entity.News;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsRepository extends JpaRepository<News, Long> {

    Page<News> findAllByActiveTrueOrderByPublishedAtDesc(Pageable pageable);
}