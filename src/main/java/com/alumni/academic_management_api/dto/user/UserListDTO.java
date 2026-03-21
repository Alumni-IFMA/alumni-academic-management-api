package com.alumni.academic_management_api.dto.user;

import com.alumni.academic_management_api.entity.AcademicProfile;
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

public class UserListDTO implements Serializable {

    String name;

    List<AcademicProfile> academicProfiles;

    String status;

}