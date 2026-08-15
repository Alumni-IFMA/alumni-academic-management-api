package com.alumni.academic_management_api.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Dados do alumni retornados na busca de usuários")
public class AlumniSearchResponseDTO implements Serializable {

    @Schema(description = "Nome completo do alumni", example = "João da Silva")
    String name;

    @Schema(description = "URL da foto de perfil armazenada no MinIO")
    String profilePictureUrl;

    @Schema(description = "Cargo ou posição profissional atual")
    String currentPosition;

    @Schema(description = "Nome do campus do alumni", example = "Campus São Luís")
    String campus;

    @Schema(description = "Nome do curso do alumni", example = "Ciência da Computação")
    String course;
}
