package com.alumni.academic_management_api.service;

import com.alumni.academic_management_api.dto.user.RegisterRequestDTO;
import com.alumni.academic_management_api.dto.user.UserProfileResponseDTO;
import com.alumni.academic_management_api.dto.user.UserSimpleDTO;
import com.alumni.academic_management_api.entity.AcademicProfile;
import com.alumni.academic_management_api.entity.CampusCourse;
import com.alumni.academic_management_api.entity.User;
import com.alumni.academic_management_api.enums.AccountStatus;
import com.alumni.academic_management_api.enums.Role;
import com.alumni.academic_management_api.exception.ResourceNotFoundException;
import com.alumni.academic_management_api.mapper.UserMapper;
import com.alumni.academic_management_api.repository.AcademicProfileRepository;
import com.alumni.academic_management_api.repository.CampusesCourseRepository;
import com.alumni.academic_management_api.repository.UserRepository;
import com.alumni.academic_management_api.service.validation.UserValidator;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Transactional
@Service
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final UserValidator userValidator;
    private final AcademicProfileRepository academicProfileRepository;
    private final CampusesCourseRepository campusesCourseRepository;

    public UserService(
            UserRepository userRepository,
            UserMapper userMapper,
            UserValidator userValidator,
            AcademicProfileRepository academicProfileRepository,
            CampusesCourseRepository campusesCourseRepository
    ) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.userValidator = userValidator;
        this.academicProfileRepository = academicProfileRepository;
        this.campusesCourseRepository = campusesCourseRepository;
    }

    public UserSimpleDTO createUser(RegisterRequestDTO requestDTO) {
        userValidator.validateCreate(requestDTO);

        User user = userMapper.toEntity(requestDTO);

        user.setAccountStatus(AccountStatus.PENDING_VERIFICATION);
        user.setRole(Role.ALUMNI);

        User savedUser = userRepository.save(user);

        CampusCourse campusCourse = campusesCourseRepository.findById(requestDTO.getCampusCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Campus course not found with id: " +
                        requestDTO.getCampusCourseId()));

        AcademicProfile academicProfile = AcademicProfile.builder()
                .user(savedUser)
                .campusCourse(campusCourse)
                .entryYear(requestDTO.getEntryYear())
                .conclusionYear(requestDTO.getConclusionYear())
                .build();

        academicProfileRepository.save(academicProfile);

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

    public UserSimpleDTO findUserById(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        return userMapper.toSimpleDTO(user);
    }
}
