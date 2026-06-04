package com.alumni.academic_management_api.dto.news;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "Dados para criação ou atualização de uma notícia")
public class NewsRequestDTO implements Serializable {

    @NotBlank
    @Schema(description = "Título da notícia", example = "IFSC lança nova plataforma de egressos")
    private String title;

    @Schema(description = "Resumo curto exibido em listagens")
    private String summary;

    @NotBlank
    @Schema(description = "Conteúdo completo da notícia em texto ou HTML")
    private String content;

    @Schema(description = "URL da imagem de capa (preenchida pelo endpoint de upload)")
    private String coverImageUrl;

    @Schema(description = "Data e hora de publicação. Null = publicar imediatamente")
    private LocalDateTime publishedAt;
}
