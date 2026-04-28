package com.alumni.academic_management_api.dto.user;

import com.alumni.academic_management_api.enums.Role;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRoleRequestDTO implements Serializable {

    @NotNull
    private Role role;
}
