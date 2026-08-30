package com.alumni.academic_management_api.mapper;

import com.alumni.academic_management_api.dto.user.UserProfileResponseDTO;
import com.alumni.academic_management_api.dto.user.UserSimpleDTO;
import com.alumni.academic_management_api.entity.User;
import com.alumni.academic_management_api.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.List;
import java.util.stream.Collectors;

public abstract class UserMapperDecorator implements UserMapper {

    @Autowired
    @Qualifier("delegate")
    protected UserMapper delegate;

    @Autowired
    protected FileStorageService fileStorageService;

    @Override
    public UserSimpleDTO toSimpleDTO(User user) {
        UserSimpleDTO dto = delegate.toSimpleDTO(user);
        if (dto != null) {
            dto.setProfilePictureUrl(toPresignedUrl(user.getProfilePictureUrl()));
        }
        return dto;
    }

    @Override
    public List<UserSimpleDTO> toSimpleDTOList(List<User> user) {
        if (user == null) {
            return null;
        }
        return user.stream().map(this::toSimpleDTO).collect(Collectors.toList());
    }

    @Override
    public UserProfileResponseDTO toProfileDTO(User user) {
        UserProfileResponseDTO dto = delegate.toProfileDTO(user);
        if (dto != null) {
            dto.setProfilePictureUrl(toPresignedUrl(user.getProfilePictureUrl()));
        }
        return dto;
    }

    private String toPresignedUrl(String objectKey) {
        if (objectKey == null) {
            return null;
        }
        return fileStorageService.generatePresignedUrl(objectKey, 15);
    }
}
