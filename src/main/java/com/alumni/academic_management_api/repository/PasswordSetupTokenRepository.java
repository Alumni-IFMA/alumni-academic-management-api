package com.alumni.academic_management_api.repository;

import com.alumni.academic_management_api.entity.PasswordSetupToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordSetupTokenRepository extends JpaRepository<PasswordSetupToken, Long> {
}
