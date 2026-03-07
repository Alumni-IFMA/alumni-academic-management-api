package com.alumni.academic_management_api.mapper;

import com.alumni.academic_management_api.dto.campusescourses.CampusCourseResponseDTO;
import com.alumni.academic_management_api.entity.CampusCourse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CampusCourseMapper {

    @Mapping(source = "campus.name", target = "campusName")
    @Mapping(source = "course.name", target = "courseName")
    @Mapping(source = "course.level", target = "level")
    @Mapping(source = "course.modality", target = "modality")
    CampusCourseResponseDTO toDTO(CampusCourse entity);

    List<CampusCourseResponseDTO> toDTOList(List<CampusCourse> entities);
}
