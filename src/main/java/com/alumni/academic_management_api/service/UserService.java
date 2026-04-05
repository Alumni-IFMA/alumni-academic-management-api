package com.alumni.academic_management_api.service;

import com.alumni.academic_management_api.dto.user.RegisterRequestDTO;
import com.alumni.academic_management_api.dto.user.UserProfileResponseDTO;
import com.alumni.academic_management_api.dto.user.UserSimpleDTO;
import com.alumni.academic_management_api.entity.User;
import com.alumni.academic_management_api.enums.AccountStatus;
import com.alumni.academic_management_api.exception.ResourceNotFoundException;
import com.alumni.academic_management_api.mapper.UserMapper;
import com.alumni.academic_management_api.repository.UserRepository;
import com.alumni.academic_management_api.service.validation.UserValidator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserValidator userValidator;

    public UserService(
            UserRepository userRepository,
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            UserValidator userValidator
    ) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.userValidator = userValidator;
    }

    public UserSimpleDTO createUser(RegisterRequestDTO requestDTO) {
        userValidator.validateCreate(requestDTO);

        User user = userMapper.toEntity(requestDTO);

        user.setAccountStatus(AccountStatus.PENDING_VERIFICATION);

        User savedUser = userRepository.save(user);

        return userMapper.toSimpleDTO(savedUser);
    }

    public List<UserSimpleDTO> findAll() {

        List<User> userList = userRepository.findAll();

        return userMapper.toSimpleDTOList(userList);
    }

    public UserProfileResponseDTO getUserProfile(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        return userMapper.toProfileDTO(user);
    }
}
