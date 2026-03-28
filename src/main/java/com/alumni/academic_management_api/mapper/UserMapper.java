package com.alumni.academic_management_api.mapper;

import com.alumni.academic_management_api.dto.user.RegisterRequestDTO;
import com.alumni.academic_management_api.dto.user.UserSimpleDTO;
import com.alumni.academic_management_api.entity.User;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {

    User toEntity(RegisterRequestDTO dto);

    UserSimpleDTO toSimpleDTO(User user);

    List<UserSimpleDTO> toSimpleDTOList(List<User> user);
}
