package com.alumni.academic_management_api.repository;

import com.alumni.academic_management_api.entity.AcademicProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AcademicProfileRepository extends JpaRepository<AcademicProfile, Long> {

}