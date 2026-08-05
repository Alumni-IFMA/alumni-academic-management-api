package com.alumni.academic_management_api.dto.news;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "Dados para criação ou atualização de uma notícia (multipart/form-data)")
public class NewsRequestDTO {

    @NotBlank
    @Schema(description = "Título da notícia", example = "IFSC lança nova plataforma de egressos")
    private String title;

    @Schema(description = "Resumo curto exibido em listagens")
    private String summary;

    @NotBlank
    @Schema(description = "Conteúdo completo da notícia em texto ou HTML")
    private String content;

    @Schema(description = "Imagem de capa (opcional). Se omitida na atualização, mantém a imagem atual.",
            type = "string", format = "binary")
    private MultipartFile coverImage;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Schema(description = "Data e hora de publicação. Null = publicar imediatamente")
    private LocalDateTime publishedAt;

    @Schema(description = "Se true, a notícia fica visível apenas para administradores")
    private boolean draft;
}
