package com.alumni.academic_management_api.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Dados para cadastro de um novo usuário")
public class RegisterRequestDTO implements Serializable {

    @Schema(description = "Nome completo do usuário", example = "João da Silva")
    String name;

    @Schema(description = "CPF do usuário (somente dígitos)", example = "12345678901")
    String cpf;

    @Schema(description = "E-mail institucional ou pessoal", example = "joao@example.com")
    String email;

    @Schema(description = "ID do vínculo campus-curso (ver GET /campus-courses)", example = "3")
    Long campusCourseId;

    @Schema(description = "Ano de ingresso no curso", example = "2018")
    Integer entryYear;

    @Schema(description = "Ano de conclusão do curso (null se ainda cursando)", example = "2022")
    Integer conclusionYear;
}
