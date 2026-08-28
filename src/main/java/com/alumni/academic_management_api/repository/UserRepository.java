package com.alumni.academic_management_api.repository;

import com.alumni.academic_management_api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByCpf(String cpf);
    Optional<User> findByEmail(String email);

    @Query("""
            SELECT DISTINCT user
            FROM User user
            LEFT JOIN FETCH user.academicProfiles academicProfile
            LEFT JOIN FETCH academicProfile.campusCourse campusCourse
            LEFT JOIN FETCH campusCourse.campus campus
            LEFT JOIN FETCH campusCourse.course course
            WHERE (:campusId IS NULL OR campus.id = :campusId)
              AND (:courseId IS NULL OR course.id = :courseId)
              AND (:name IS NULL OR :name = ''
                   OR LOWER(user.name) LIKE LOWER(CONCAT('%', :name, '%')))
              AND user.accountStatus = com.alumni.academic_management_api.enums.AccountStatus.ACTIVE
            ORDER BY user.name ASC
            """)
    List<User> searchUsers(
            @Param("campusId") Long campusId,
            @Param("courseId") Long courseId,
            @Param("name") String name
    );

    List<User> findDistinctByAcademicProfilesCampusCourseIdInAndIdNot(
            Collection<Long> campusCourseIds,
            Long userId
    );
}
