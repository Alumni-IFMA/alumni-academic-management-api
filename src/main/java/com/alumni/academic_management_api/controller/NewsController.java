package com.alumni.academic_management_api.controller;

import com.alumni.academic_management_api.config.OpenApiConfig;
import com.alumni.academic_management_api.dto.news.NewsRequestDTO;
import com.alumni.academic_management_api.dto.news.NewsResponseDTO;
import com.alumni.academic_management_api.service.NewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/news")
@Tag(name = "Notícias", description = "Publicação e gerenciamento de notícias institucionais")
public class NewsController {

    private final NewsService newsService;

    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    @PostMapping
    @Operation(summary = "Criar notícia", description = "Acesso restrito a administradores.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Notícia criada com sucesso",
            content = @Content(schema = @Schema(implementation = NewsResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content),
        @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content),
        @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content)
    })
    public ResponseEntity<NewsResponseDTO> create(@RequestBody @Valid NewsRequestDTO request) {
        log.debug("REST request to create news");
        return ResponseEntity.status(HttpStatus.CREATED).body(newsService.create(request));
    }

    @GetMapping
    @Operation(summary = "Listar notícias paginadas", description = "Público. Suporta parâmetros page, size e sort.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista paginada retornada com sucesso. "
                + "O objeto de resposta segue a estrutura Page do Spring: "
                + "content (array de NewsResponseDTO), totalElements, totalPages, pageable.",
            content = @Content(schema = @Schema(implementation = NewsResponseDTO.class)))
    })
    public ResponseEntity<Page<NewsResponseDTO>> findAll(Pageable pageable) {
        log.debug("REST request to list news");
        return ResponseEntity.ok(newsService.findAll(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar notícia por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Notícia encontrada",
            content = @Content(schema = @Schema(implementation = NewsResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Notícia não encontrada", content = @Content)
    })
    public ResponseEntity<NewsResponseDTO> findById(
            @Parameter(description = "ID da notícia") @PathVariable Long id) {
        log.debug("REST request to get news: {}", id);
        return ResponseEntity.ok(newsService.findById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar notícia", description = "Acesso restrito a administradores.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Notícia atualizada",
            content = @Content(schema = @Schema(implementation = NewsResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content),
        @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content),
        @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content),
        @ApiResponse(responseCode = "404", description = "Notícia não encontrada", content = @Content)
    })
    public ResponseEntity<NewsResponseDTO> update(
            @Parameter(description = "ID da notícia") @PathVariable Long id,
            @RequestBody @Valid NewsRequestDTO request) {
        log.debug("REST request to update news: {}", id);
        return ResponseEntity.ok(newsService.update(id, request));
    }

    @PostMapping("/{id}/cover-image")
    @Operation(summary = "Upload de imagem de capa",
        description = "Envia imagem para MinIO e atualiza a URL da notícia. Acesso restrito a administradores.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "URL da imagem retornada",
            content = @Content(schema = @Schema(example = "{\"coverImageUrl\": \"https://...\"}"))),
        @ApiResponse(responseCode = "400", description = "Arquivo inválido", content = @Content),
        @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content),
        @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content),
        @ApiResponse(responseCode = "404", description = "Notícia não encontrada", content = @Content)
    })
    public ResponseEntity<Map<String, String>> uploadCoverImage(
            @Parameter(description = "ID da notícia") @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        log.debug("REST request to upload cover image for news: {}", id);
        String url = newsService.uploadCoverImage(id, file);
        return ResponseEntity.ok(Map.of("coverImageUrl", url));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir notícia", description = "Acesso restrito a administradores.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Notícia excluída com sucesso", content = @Content),
        @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content),
        @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content),
        @ApiResponse(responseCode = "404", description = "Notícia não encontrada", content = @Content)
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID da notícia") @PathVariable Long id) {
        log.debug("REST request to delete news: {}", id);
        newsService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
