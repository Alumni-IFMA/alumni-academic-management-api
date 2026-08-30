package com.alumni.academic_management_api.dto.user;

import com.alumni.academic_management_api.enums.AccountStatus;
import com.alumni.academic_management_api.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Dados simplificados do usuário")
public class UserSimpleDTO implements Serializable {

    @Schema(description = "ID único do usuário", example = "1")
    Long id;

    @Schema(description = "Nome completo", example = "João da Silva")
    String name;

    @Schema(description = "E-mail", example = "joao@example.com")
    String email;

    @Setter
    @Schema(description = "URL assinada (temporária) da foto de perfil armazenada no bucket")
    String profilePictureUrl;

    List<AcademicProfileResponseDTO> academicProfiles;

    @Schema(description = "Status da conta: ACTIVE, INACTIVE ou PENDING")
    AccountStatus status;

    @Schema(description = "Papel do usuário: ROLE_USER ou ROLE_ADMIN")
    Role role;
}
