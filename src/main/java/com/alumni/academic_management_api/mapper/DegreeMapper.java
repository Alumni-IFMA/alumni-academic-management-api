package com.alumni.academic_management_api.mapper;

import com.alumni.academic_management_api.dto.degree.DegreeResponseDTO;
import com.alumni.academic_management_api.entity.Degree;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DegreeMapper {

    @Mapping(target = "userId", source = "user.id")
    DegreeResponseDTO toResponseDTO(Degree degree);
}
