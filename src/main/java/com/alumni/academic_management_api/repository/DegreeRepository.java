package com.alumni.academic_management_api.repository;

import com.alumni.academic_management_api.entity.Degree;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DegreeRepository extends JpaRepository<Degree, Long> {

    List<Degree> findByUserId(Long userId);
}
