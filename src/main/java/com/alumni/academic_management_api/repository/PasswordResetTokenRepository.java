package com.alumni.academic_management_api.repository;

import com.alumni.academic_management_api.entity.PasswordResetToken;
import com.alumni.academic_management_api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
    
    void deleteByUser(User user);

}
