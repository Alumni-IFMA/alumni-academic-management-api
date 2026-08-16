package com.alumni.academic_management_api.controller;

import com.alumni.academic_management_api.config.OpenApiConfig;
import com.alumni.academic_management_api.dto.support.SupportMessageRequestDTO;
import com.alumni.academic_management_api.dto.support.SupportMessageResponseDTO;
import com.alumni.academic_management_api.service.SupportMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/support")
public class SupportMessageController {

    private final SupportMessageService supportMessageService;

    public SupportMessageController(SupportMessageService supportMessageService) {
        this.supportMessageService = supportMessageService;
    }

    @PostMapping
    @Operation(summary = "Enviar mensagem de suporte")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Mensagem enviada com sucesso"),
        @ApiResponse(responseCode = "400", description = "Corpo da requisição inválido", content = @Content),
        @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content)
    })
    public ResponseEntity<SupportMessageResponseDTO> send(
            @RequestBody @Valid SupportMessageRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.debug("REST request to send support message");
        SupportMessageResponseDTO response = supportMessageService.send(userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Listar mensagens de suporte", description = "Acesso restrito a administradores.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Busca realizada com sucesso"),
        @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content),
        @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content)
    })
    public ResponseEntity<Page<SupportMessageResponseDTO>> findAll(
            @Parameter(description = "Filtra por mensagens resolvidas ou não")
            @RequestParam(required = false) Boolean resolved,
            Pageable pageable) {
        log.debug("REST request to list support messages: resolved={}", resolved);
        return ResponseEntity.ok(supportMessageService.findAll(resolved, pageable));
    }

    @PatchMapping("/{id}/resolve")
    @Operation(summary = "Resolver mensagem de suporte", description = "Acesso restrito a administradores.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Mensagem marcada como resolvida"),
        @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content),
        @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content),
        @ApiResponse(responseCode = "404", description = "Mensagem não encontrada", content = @Content)
    })
    public ResponseEntity<SupportMessageResponseDTO> resolve(
            @Parameter(description = "ID da mensagem") @PathVariable Long id) {
        log.debug("REST request to resolve support message: {}", id);
        return ResponseEntity.ok(supportMessageService.resolve(id));
    }
}
