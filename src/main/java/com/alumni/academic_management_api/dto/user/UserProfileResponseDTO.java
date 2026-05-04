package com.alumni.academic_management_api.dto.user;

import com.alumni.academic_management_api.enums.AccountStatus;
import com.alumni.academic_management_api.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileResponseDTO implements Serializable {

    Long id;

    String name;

    String email;

    String bio;

    String profilePictureUrl;

    String linkedinUrl;

    String portfolioUrl;

    String currentPosition;

    AccountStatus accountStatus;

    Role role;

    List<AcademicProfileResponseDTO> academicProfiles;
}
