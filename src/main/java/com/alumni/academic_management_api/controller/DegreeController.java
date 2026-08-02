package com.alumni.academic_management_api.controller;

import com.alumni.academic_management_api.config.OpenApiConfig;
import com.alumni.academic_management_api.dto.degree.DegreeRequestDTO;
import com.alumni.academic_management_api.dto.degree.DegreeResponseDTO;
import com.alumni.academic_management_api.service.DegreeService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/degrees")
@Tag(name = "Diplomas", description = "Upload e download de diplomas dos egressos")
public class DegreeController {

    private final DegreeService degreeService;

    public DegreeController(DegreeService degreeService) {
        this.degreeService = degreeService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload de diploma", description = "Envia um diploma em PDF para um usuário. "
            + "Acesso restrito a administradores.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Diploma cadastrado com sucesso",
            content = @Content(schema = @Schema(implementation = DegreeResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Arquivo inválido (não é PDF) ou dados inválidos",
            content = @Content),
        @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content),
        @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content),
        @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content)
    })
    public ResponseEntity<DegreeResponseDTO> upload(@ModelAttribute @Valid DegreeRequestDTO request) {
        log.debug("REST request to upload degree for user: {}", request.getUserId());

        return ResponseEntity.status(HttpStatus.CREATED).body(degreeService.upload(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Listar meus diplomas", description = "Lista os diplomas do usuário autenticado.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de diplomas retornada com sucesso",
            content = @Content(schema = @Schema(implementation = DegreeResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content)
    })
    public ResponseEntity<List<DegreeResponseDTO>> findMine(@AuthenticationPrincipal UserDetails userDetails) {
        log.debug("REST request to list degrees for authenticated user");

        return ResponseEntity.ok(degreeService.findByAuthenticatedUser(userDetails.getUsername()));
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Baixar diploma", description = "Gera uma URL presignada do MinIO (válida por 15 minutos) "
            + "para o dono do diploma baixar o arquivo.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "URL de download retornada",
            content = @Content(schema = @Schema(example = "{\"downloadUrl\": \"https://...\"}"))),
        @ApiResponse(responseCode = "400", description = "Diploma não pertence ao usuário autenticado",
            content = @Content),
        @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content),
        @ApiResponse(responseCode = "404", description = "Diploma não encontrado", content = @Content)
    })
    public ResponseEntity<Map<String, String>> download(
            @Parameter(description = "ID do diploma") @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.debug("REST request to download degree: {}", id);

        String url = degreeService.generateDownloadUrl(id, userDetails.getUsername());

        return ResponseEntity.ok(Map.of("downloadUrl", url));
    }
}
