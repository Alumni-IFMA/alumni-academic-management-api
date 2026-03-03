package com.alumni.academic_management_api.service.validation;

import com.alumni.academic_management_api.dto.user.RegisterRequestDTO;
import com.alumni.academic_management_api.exception.BusinessException;
import com.alumni.academic_management_api.repository.UserRepository;
import org.springframework.stereotype.Component;

@Component
public class UserValidator {
    private final UserRepository userRepository;

    public UserValidator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void validateCreate(RegisterRequestDTO requestDTO) {
        if (userRepository.existsByCpf(requestDTO.getCpf())) {
            throw new BusinessException("User already exists");
        }
    }
}
