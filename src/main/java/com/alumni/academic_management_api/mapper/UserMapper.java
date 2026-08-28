package com.alumni.academic_management_api.mapper;

import com.alumni.academic_management_api.dto.user.AcademicProfileResponseDTO;
import com.alumni.academic_management_api.dto.user.AlumniSearchResponseDTO;
import com.alumni.academic_management_api.dto.user.RegisterRequestDTO;
import com.alumni.academic_management_api.dto.user.UserProfileResponseDTO;
import com.alumni.academic_management_api.dto.user.UserSimpleDTO;
import com.alumni.academic_management_api.entity.AcademicProfile;
import com.alumni.academic_management_api.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {

    User toEntity(RegisterRequestDTO dto);

    @Mapping(source = "accountStatus", target = "status")
    UserSimpleDTO toSimpleDTO(User user);

    List<UserSimpleDTO> toSimpleDTOList(List<User> user);

    default AlumniSearchResponseDTO toAlumniSearchResponseDTO(User user) {
        AcademicProfile academicProfile = user.getAcademicProfiles().stream().findFirst().orElse(null);

        return AlumniSearchResponseDTO.builder()
                .name(user.getName())
                .profilePictureUrl(user.getProfilePictureUrl())
                .currentPosition(user.getCurrentPosition())
                .campus(academicProfile == null ? null : academicProfile.getCampusCourse().getCampus().getName())
                .course(academicProfile == null ? null : academicProfile.getCampusCourse().getCourse().getName())
                .build();
    }

    UserProfileResponseDTO toProfileDTO(User user);

    @Mapping(source = "campusCourse.campus.name", target = "campusName")
    @Mapping(source = "campusCourse.course.name", target = "courseName")
    @Mapping(source = "campusCourse.course.level", target = "level")
    @Mapping(source = "campusCourse.course.modality", target = "modality")
    AcademicProfileResponseDTO toAcademicProfileDTO(AcademicProfile profile);
}
