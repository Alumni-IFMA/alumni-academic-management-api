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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/news")
@Tag(name = "Notícias", description = "Publicação e gerenciamento de notícias institucionais")
public class NewsController {

    private static final String ADMIN_AUTHORITY = "ROLE_ADMIN";

    private final NewsService newsService;

    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Criar notícia", description = "Acesso restrito a administradores.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Notícia criada com sucesso",
            content = @Content(schema = @Schema(implementation = NewsResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content),
        @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content),
        @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content)
    })
    public ResponseEntity<NewsResponseDTO> create(@ModelAttribute @Valid NewsRequestDTO request) {
        log.debug("REST request to create news");
        return ResponseEntity.status(HttpStatus.CREATED).body(newsService.create(request));
    }

    @GetMapping
    @Operation(summary = "Listar notícias paginadas", description = "Público. Suporta parâmetros page, size e sort. "
            + "Administradores autenticados também veem rascunhos.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista paginada retornada com sucesso. "
                + "O objeto de resposta segue a estrutura Page do Spring: "
                + "content (array de NewsResponseDTO), totalElements, totalPages, pageable.",
            content = @Content(schema = @Schema(implementation = NewsResponseDTO.class)))
    })
    public ResponseEntity<Page<NewsResponseDTO>> findAll(
            Pageable pageable, @AuthenticationPrincipal UserDetails userDetails) {
        log.debug("REST request to list news");
        return ResponseEntity.ok(newsService.findAll(pageable, isAdmin(userDetails)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar notícia por ID", description = "Público. Rascunhos só são "
            + "visíveis para administradores autenticados.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Notícia encontrada",
            content = @Content(schema = @Schema(implementation = NewsResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Notícia não encontrada", content = @Content)
    })
    public ResponseEntity<NewsResponseDTO> findById(
            @Parameter(description = "ID da notícia") @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.debug("REST request to get news: {}", id);
        return ResponseEntity.ok(newsService.findById(id, isAdmin(userDetails)));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
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
            @ModelAttribute @Valid NewsRequestDTO request) {
        log.debug("REST request to update news: {}", id);
        return ResponseEntity.ok(newsService.update(id, request));
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

    private boolean isAdmin(UserDetails userDetails) {
        return userDetails != null && userDetails.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals(ADMIN_AUTHORITY));
    }
}
