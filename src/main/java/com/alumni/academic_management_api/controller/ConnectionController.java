package com.alumni.academic_management_api.controller;

import com.alumni.academic_management_api.config.OpenApiConfig;
import com.alumni.academic_management_api.dto.connection.ConnectionRequestDTO;
import com.alumni.academic_management_api.dto.connection.ConnectionResponseDTO;
import com.alumni.academic_management_api.dto.user.UserSimpleDTO;
import com.alumni.academic_management_api.service.ConnectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/connections")
@Tag(name = "Conexões", description = "Solicitações e conexões entre usuários")
public class ConnectionController {

    private final ConnectionService connectionService;

    public ConnectionController(ConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    @PostMapping
    @Operation(summary = "Enviar solicitação de conexão")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Solicitação enviada com sucesso",
            content = @Content(schema = @Schema(implementation = ConnectionResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Solicitação inválida ou conexão já existente",
            content = @Content),
        @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content),
        @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content)
    })
    public ResponseEntity<ConnectionResponseDTO> sendRequest(
            @RequestBody @Valid ConnectionRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        log.debug("REST request to send connection request to user: {}", request.getAddresseeId());
        ConnectionResponseDTO response = connectionService.sendRequest(
                userDetails.getUsername(), request.getAddresseeId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Listar conexões aceitas do usuário autenticado")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso",
            content = @Content(array = @ArraySchema(
                    schema = @Schema(implementation = ConnectionResponseDTO.class)))),
        @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content),
        @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content)
    })
    public ResponseEntity<List<ConnectionResponseDTO>> findAcceptedConnections(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        log.debug("REST request to list accepted connections");
        return ResponseEntity.ok(connectionService.findAcceptedConnections(userDetails.getUsername()));
    }

    @GetMapping("/pending")
    @Operation(summary = "Listar solicitações de conexão recebidas e pendentes")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso",
            content = @Content(array = @ArraySchema(
                    schema = @Schema(implementation = ConnectionResponseDTO.class)))),
        @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content),
        @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content)
    })
    public ResponseEntity<List<ConnectionResponseDTO>> findPendingReceivedRequests(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        log.debug("REST request to list pending received connection requests");
        return ResponseEntity.ok(connectionService.findPendingReceivedRequests(userDetails.getUsername()));
    }

    @GetMapping("/sent")
    @Operation(summary = "Listar solicitações de conexão enviadas e pendentes")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso",
            content = @Content(array = @ArraySchema(
                    schema = @Schema(implementation = ConnectionResponseDTO.class)))),
        @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content),
        @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content)
    })
    public ResponseEntity<List<ConnectionResponseDTO>> findSentRequests(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        log.debug("REST request to list sent pending connection requests");
        return ResponseEntity.ok(connectionService.findSentRequests(userDetails.getUsername()));
    }

    @GetMapping("/suggestions")
    @Operation(summary = "Listar sugestões de conexão", description = "Retorna uma página de sugestões de conexão "
            + "para o usuário autenticado, com base nos cursos em comum.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Página de sugestões retornada com sucesso"),
        @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content),
        @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content)
    })
    public ResponseEntity<Page<UserSimpleDTO>> findSuggestions(
            @AuthenticationPrincipal UserDetails userDetails,
            Pageable pageable
    ) {
        log.debug("REST request to list connection suggestions");
        return ResponseEntity.ok(connectionService.findSuggestions(userDetails.getUsername(), pageable));
    }

    @PatchMapping("/{id}/accept")
    @Operation(summary = "Aceitar solicitação de conexão")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Solicitação aceita com sucesso",
            content = @Content(schema = @Schema(implementation = ConnectionResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Usuário não pode aceitar esta solicitação",
            content = @Content),
        @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content),
        @ApiResponse(responseCode = "404", description = "Conexão ou usuário não encontrado", content = @Content)
    })
    public ResponseEntity<ConnectionResponseDTO> acceptRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        log.debug("REST request to accept connection request: {}", id);
        return ResponseEntity.ok(connectionService.acceptRequest(id, userDetails.getUsername()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Recusar ou desfazer conexão")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Conexão removida com sucesso"),
        @ApiResponse(responseCode = "400", description = "Usuário não participa desta conexão",
            content = @Content),
        @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content),
        @ApiResponse(responseCode = "404", description = "Conexão ou usuário não encontrado", content = @Content)
    })
    public ResponseEntity<Void> deleteConnection(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        log.debug("REST request to delete connection: {}", id);
        connectionService.deleteConnection(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
