package com.alumni.academic_management_api.dto.user;

import com.alumni.academic_management_api.enums.AccountStatus;
import com.alumni.academic_management_api.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Perfil completo do usuário com dados profissionais e acadêmicos")
public class UserProfileResponseDTO implements Serializable {

    @Schema(description = "ID único do usuário", example = "1")
    Long id;

    @Schema(description = "Nome completo", example = "João da Silva")
    String name;

    @Schema(description = "E-mail", example = "joao@example.com")
    String email;

    @Schema(description = "Biografia ou apresentação pessoal")
    String bio;

    @Schema(description = "URL assinada (temporária) da foto de perfil armazenada no bucket")
    String profilePictureUrl;

    @Schema(description = "URL do perfil no LinkedIn")
    String linkedinUrl;

    @Schema(description = "URL do portfólio pessoal ou GitHub")
    String portfolioUrl;

    @Schema(description = "Cargo ou posição profissional atual")
    String currentPosition;

    @Schema(description = "Status da conta")
    AccountStatus accountStatus;

    @Schema(description = "Papel do usuário")
    Role role;

    @Schema(description = "Lista de perfis acadêmicos")
    List<AcademicProfileResponseDTO> academicProfiles;

    @Schema(description = "Indica se o usuário já concluiu o tutorial de primeiro acesso", example = "true")
    private Boolean hasSeenTutorial;
}
