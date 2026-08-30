package com.alumni.academic_management_api.mapper;

import com.alumni.academic_management_api.dto.user.AcademicProfileResponseDTO;
import com.alumni.academic_management_api.dto.user.RegisterRequestDTO;
import com.alumni.academic_management_api.dto.user.UserProfileResponseDTO;
import com.alumni.academic_management_api.dto.user.UserSimpleDTO;
import com.alumni.academic_management_api.entity.AcademicProfile;
import com.alumni.academic_management_api.entity.User;
import com.alumni.academic_management_api.service.FileStorageService;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Mapper(componentModel = "spring")
public abstract class UserMapper {

    @Autowired
    protected FileStorageService fileStorageService;

    public abstract User toEntity(RegisterRequestDTO dto);

    @Mapping(source = "accountStatus", target = "status")
    public abstract UserSimpleDTO toSimpleDTO(User user);

    public abstract List<UserSimpleDTO> toSimpleDTOList(List<User> user);

    public abstract UserProfileResponseDTO toProfileDTO(User user);

    @Mapping(source = "campusCourse.campus.name", target = "campusName")
    @Mapping(source = "campusCourse.course.name", target = "courseName")
    @Mapping(source = "campusCourse.course.level", target = "level")
    @Mapping(source = "campusCourse.course.modality", target = "modality")
    public abstract AcademicProfileResponseDTO toAcademicProfileDTO(AcademicProfile profile);

    @AfterMapping
    protected void mapProfilePictureUrlOnSimpleDTO(User user, @MappingTarget UserSimpleDTO.UserSimpleDTOBuilder dto) {
        dto.profilePictureUrl(toPresignedUrl(user.getProfilePictureUrl()));
    }

    @AfterMapping
    protected void mapProfilePictureUrlOnProfileDTO(
            User user, @MappingTarget UserProfileResponseDTO.UserProfileResponseDTOBuilder dto) {
        dto.profilePictureUrl(toPresignedUrl(user.getProfilePictureUrl()));
    }

    private String toPresignedUrl(String objectKey) {
        if (objectKey == null) {
            return null;
        }
        return fileStorageService.generatePresignedUrl(objectKey, 15);
    }
}
