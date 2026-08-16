package com.alumni.academic_management_api.mapper;

import com.alumni.academic_management_api.dto.support.SupportMessageResponseDTO;
import com.alumni.academic_management_api.entity.SupportMessage;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SupportMessageMapper {

    SupportMessageResponseDTO toResponseDTO(SupportMessage entity);
}
