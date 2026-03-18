package com.alumni.academic_management_api.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor

public class UserListDTO implements Serializable {

    String name;

    String academicProfiles;

    String status;

}