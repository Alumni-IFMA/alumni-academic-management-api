package com.alumni.academic_management_api.repository;

import com.alumni.academic_management_api.entity.User;
import com.alumni.academic_management_api.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByCpf(String cpf);
    Optional<User> findByEmail(String email);
    long countByRole(Role role);
}
