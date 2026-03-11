package com.alumni.academic_management_api.repository;

import com.alumni.academic_management_api.entity.CampusCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CampusesCourseRepository extends JpaRepository<CampusCourse, Long> {
}
