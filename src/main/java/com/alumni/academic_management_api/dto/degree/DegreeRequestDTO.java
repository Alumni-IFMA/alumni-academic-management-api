package com.alumni.academic_management_api.dto.degree;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "Dados para upload de um diploma (multipart/form-data)")
public class DegreeRequestDTO {

    @NotNull
    @Schema(description = "ID do usuário dono do diploma", example = "1")
    private Long userId;

    @NotBlank
    @Schema(description = "Título/nome do diploma", example = "Bacharelado em Ciência da Computação")
    private String title;

    @Schema(description = "Arquivo do diploma em PDF", type = "string", format = "binary")
    private MultipartFile file;
}
