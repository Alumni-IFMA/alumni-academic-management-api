package com.alumni.academic_management_api.dto.news;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Dados de uma notícia retornados pela API")
public class NewsResponseDTO implements Serializable {

    @Schema(description = "ID único da notícia", example = "42")
    private Long id;

    @Schema(description = "Título da notícia")
    private String title;

    @Schema(description = "Resumo")
    private String summary;

    @Schema(description = "Conteúdo completo")
    private String content;

    @Schema(description = "URL da imagem de capa")
    private String coverImageUrl;

    @Schema(description = "Data e hora de publicação")
    private LocalDateTime publishedAt;

    @Schema(description = "Indica se a notícia está ativa/visível")
    private boolean active;

    @Schema(description = "Data e hora de criação do registro")
    private LocalDateTime createdAt;
}
