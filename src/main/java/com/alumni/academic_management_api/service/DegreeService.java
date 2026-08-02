package com.alumni.academic_management_api.service;

import com.alumni.academic_management_api.dto.degree.DegreeRequestDTO;
import com.alumni.academic_management_api.dto.degree.DegreeResponseDTO;
import com.alumni.academic_management_api.entity.Degree;
import com.alumni.academic_management_api.entity.User;
import com.alumni.academic_management_api.exception.BusinessException;
import com.alumni.academic_management_api.exception.ResourceNotFoundException;
import com.alumni.academic_management_api.mapper.DegreeMapper;
import com.alumni.academic_management_api.repository.DegreeRepository;
import com.alumni.academic_management_api.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Transactional
@Service
public class DegreeService {

    private final DegreeRepository degreeRepository;
    private final UserRepository userRepository;
    private final DegreeMapper degreeMapper;
    private final FileStorageService fileStorageService;

    public DegreeService(
            DegreeRepository degreeRepository,
            UserRepository userRepository,
            DegreeMapper degreeMapper,
            FileStorageService fileStorageService
    ) {
        this.degreeRepository = degreeRepository;
        this.userRepository = userRepository;
        this.degreeMapper = degreeMapper;
        this.fileStorageService = fileStorageService;
    }

    public DegreeResponseDTO upload(DegreeRequestDTO dto) {
        validatePdf(dto.getFile());

        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + dto.getUserId()));

        String fileUrl = fileStorageService.uploadFile(dto.getFile(), FileStorageService.FOLDER_DIPLOMAS);

        Degree degree = Degree.builder()
                .title(dto.getTitle())
                .fileUrl(fileUrl)
                .user(user)
                .build();

        return degreeMapper.toResponseDTO(degreeRepository.save(degree));
    }

    private void validatePdf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("File is required");
        }
        if (!"application/pdf".equals(file.getContentType())) {
            throw new BusinessException("Only PDF files are allowed");
        }
    }
}
