package com.alumni.academic_management_api.mapper;

import com.alumni.academic_management_api.dto.job.JobRequestDTO;
import com.alumni.academic_management_api.dto.job.JobResponseDTO;
import com.alumni.academic_management_api.entity.Job;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface JobMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Job toEntity(JobRequestDTO dto);

    JobResponseDTO toResponseDTO(Job job);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntityFromDTO(JobRequestDTO dto, @MappingTarget Job job);
}