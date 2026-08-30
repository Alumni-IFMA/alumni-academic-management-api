package com.alumni.academic_management_api.mapper;

import com.alumni.academic_management_api.dto.user.AcademicProfileResponseDTO;
import com.alumni.academic_management_api.dto.user.RegisterRequestDTO;
import com.alumni.academic_management_api.dto.user.UserProfileResponseDTO;
import com.alumni.academic_management_api.dto.user.UserSimpleDTO;
import com.alumni.academic_management_api.entity.AcademicProfile;
import com.alumni.academic_management_api.entity.User;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
@DecoratedWith(UserMapperDecorator.class)
public interface UserMapper {

    User toEntity(RegisterRequestDTO dto);

    @Mapping(source = "accountStatus", target = "status")
    UserSimpleDTO toSimpleDTO(User user);

    List<UserSimpleDTO> toSimpleDTOList(List<User> user);

    UserProfileResponseDTO toProfileDTO(User user);

    @Mapping(source = "campusCourse.campus.name", target = "campusName")
    @Mapping(source = "campusCourse.course.name", target = "courseName")
    @Mapping(source = "campusCourse.course.level", target = "level")
    @Mapping(source = "campusCourse.course.modality", target = "modality")
    AcademicProfileResponseDTO toAcademicProfileDTO(AcademicProfile profile);
}
