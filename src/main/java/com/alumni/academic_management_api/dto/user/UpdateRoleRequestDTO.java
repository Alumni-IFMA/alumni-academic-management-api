package com.alumni.academic_management_api.dto.user;

import com.alumni.academic_management_api.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payload para atualização do papel de um usuário")
public class UpdateRoleRequestDTO implements Serializable {

    @NotNull
    @Schema(description = "Novo papel do usuário", example = "ROLE_ADMIN")
    private Role role;
}
