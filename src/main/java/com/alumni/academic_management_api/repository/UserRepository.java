package com.alumni.academic_management_api.repository;

import com.alumni.academic_management_api.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByCpf(String cpf);
    Optional<User> findByEmail(String email);
    Page<User> findDistinctByAcademicProfilesCampusCourseIdInAndIdNotIn(
            Collection<Long> campusCourseIds,
            Collection<Long> excludedUserIds,
            Pageable pageable
    );
}
