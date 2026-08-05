package com.alumni.academic_management_api.mapper;

import com.alumni.academic_management_api.dto.connection.ConnectionResponseDTO;
import com.alumni.academic_management_api.entity.Connection;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface ConnectionMapper {

    ConnectionResponseDTO toResponseDTO(Connection connection);
}
