package com.alumni.academic_management_api.repository;

import com.alumni.academic_management_api.entity.SupportMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SupportMessageRepository extends JpaRepository<SupportMessage, Long> {

    Page<SupportMessage> findByResolved(boolean resolved, Pageable pageable);
}
