package com.alumni.academic_management_api.service;

import com.alumni.academic_management_api.dto.user.RegisterRequestDTO;
import com.alumni.academic_management_api.dto.user.AlumniSearchResponseDTO;
import com.alumni.academic_management_api.dto.user.UserProfileResponseDTO;
import com.alumni.academic_management_api.dto.user.UserSimpleDTO;
import com.alumni.academic_management_api.entity.AcademicProfile;
import com.alumni.academic_management_api.entity.CampusCourse;
import com.alumni.academic_management_api.entity.User;
import com.alumni.academic_management_api.enums.AccountStatus;
import com.alumni.academic_management_api.enums.Role;
import com.alumni.academic_management_api.exception.BusinessException;
import com.alumni.academic_management_api.exception.ResourceNotFoundException;
import com.alumni.academic_management_api.mapper.UserMapper;
import com.alumni.academic_management_api.repository.AcademicProfileRepository;
import com.alumni.academic_management_api.repository.CampusesCourseRepository;
import com.alumni.academic_management_api.repository.UserRepository;
import com.alumni.academic_management_api.service.validation.UserValidator;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Transactional
@Service
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final UserValidator userValidator;
    private final AcademicProfileRepository academicProfileRepository;
    private final CampusesCourseRepository campusesCourseRepository;
    private final FileStorageService fileStorageService;
    private final EmailService emailService;
    private final PasswordSetupTokenService passwordSetupTokenService;

    public UserService(
            UserRepository userRepository,
            UserMapper userMapper,
            UserValidator userValidator,
            AcademicProfileRepository academicProfileRepository,
            CampusesCourseRepository campusesCourseRepository,
            FileStorageService fileStorageService,
            EmailService emailService,
            PasswordSetupTokenService passwordSetupTokenService
    ) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.userValidator = userValidator;
        this.academicProfileRepository = academicProfileRepository;
        this.campusesCourseRepository = campusesCourseRepository;
        this.fileStorageService = fileStorageService;
        this.emailService = emailService;
        this.passwordSetupTokenService = passwordSetupTokenService;
    }

    public UserSimpleDTO createUser(RegisterRequestDTO requestDTO) {
        userValidator.validateCreate(requestDTO);

        User user = userMapper.toEntity(requestDTO);

        user.setAccountStatus(AccountStatus.PENDING_VERIFICATION);

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

        AcademicProfile savedAcademicProfile = academicProfileRepository.save(academicProfile);

        if (savedUser.getAcademicProfiles() != null) {
            savedUser.getAcademicProfiles().add(savedAcademicProfile);
        }

        return userMapper.toSimpleDTO(savedUser);
    }

    public List<UserSimpleDTO> findAll() {

        List<User> userList = userRepository.findAll();

        return userMapper.toSimpleDTOList(userList);
    }

    public List<AlumniSearchResponseDTO> searchUsers(Long campusId, Long courseId, String name) {
        return userRepository.searchUsers(campusId, courseId, name).stream()
                .map(userMapper::toAlumniSearchResponseDTO)
                .toList();
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

    public String uploadProfilePicture(Long userId, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        if (user.getProfilePictureUrl() != null) {
            fileStorageService.deleteFile(user.getProfilePictureUrl());
        }
        String url = fileStorageService.uploadFile(file, FileStorageService.FOLDER_PROFILE_PICTURES);
        user.setProfilePictureUrl(url);
        userRepository.save(user);
        return url;
    }

    public UserSimpleDTO updateUserRole(Long targetId, Role newRole, String authenticatedEmail) {
        User targetUser = userRepository.findById(targetId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + targetId));

        if (targetUser.getEmail().equals(authenticatedEmail)) {
            throw new BusinessException("Admin cannot change their own role");
        }

        targetUser.setRole(newRole);
        User savedUser = userRepository.save(targetUser);
        return userMapper.toSimpleDTO(savedUser);
    }

    public UserSimpleDTO approveUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (user.getAccountStatus() != AccountStatus.PENDING_VERIFICATION) {
            throw new BusinessException("Only pending verification accounts can be approved");
        }

        user.setAccountStatus(AccountStatus.ACTIVE);
        User savedUser = userRepository.save(user);

        String rawToken = passwordSetupTokenService.generateSetupToken(savedUser);
        emailService.sendAccountApprovalEmail(savedUser.getEmail(), savedUser.getName(), rawToken);

        return userMapper.toSimpleDTO(savedUser);
    }

    public void completeOnboarding(Long id, String authenticatedEmail) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (!user.getEmail().equals(authenticatedEmail)) {
            throw new BusinessException("You do not have permission to update the onboarding status of another user");
        }

        user.setHasSeenTutorial(true);
        userRepository.save(user);
    }
}
