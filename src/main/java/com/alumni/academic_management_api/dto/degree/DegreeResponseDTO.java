package com.alumni.academic_management_api.dto.degree;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "Dados de um diploma cadastrado")
public class DegreeResponseDTO implements Serializable {

    @Schema(description = "ID do diploma", example = "1")
    private Long id;

    @Schema(description = "Título/nome do diploma", example = "Bacharelado em Ciência da Computação")
    private String title;

    @Schema(description = "ID do usuário dono do diploma", example = "1")
    private Long userId;

    @Schema(description = "URL do arquivo no MinIO", example = "http://localhost:9000/alumni-files/diplomas/uuid.pdf")
    private String fileUrl;
}
