package com.alumni.academic_management_api.controller;

import com.alumni.academic_management_api.config.OpenApiConfig;
import com.alumni.academic_management_api.dto.connection.ConnectionRequestDTO;
import com.alumni.academic_management_api.dto.connection.ConnectionResponseDTO;
import com.alumni.academic_management_api.dto.user.UserSimpleDTO;
import com.alumni.academic_management_api.entity.User;
import com.alumni.academic_management_api.exception.ResourceNotFoundException;
import com.alumni.academic_management_api.repository.UserRepository;
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
    private final UserRepository userRepository;

    public ConnectionController(ConnectionService connectionService, UserRepository userRepository) {
        this.connectionService = connectionService;
        this.userRepository = userRepository;
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
        User requester = findAuthenticatedUser(userDetails);
        ConnectionResponseDTO response = connectionService.sendRequest(requester.getId(), request.getAddresseeId());
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
        User authenticatedUser = findAuthenticatedUser(userDetails);
        return ResponseEntity.ok(connectionService.findAcceptedConnections(authenticatedUser.getId()));
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
        User authenticatedUser = findAuthenticatedUser(userDetails);
        return ResponseEntity.ok(connectionService.findPendingReceivedRequests(authenticatedUser.getId()));
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
        User authenticatedUser = findAuthenticatedUser(userDetails);
        return ResponseEntity.ok(connectionService.findSentRequests(authenticatedUser.getId()));
    }

    @GetMapping("/suggestions")
    @Operation(summary = "Listar sugestões de conexão")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso",
            content = @Content(array = @ArraySchema(
                    schema = @Schema(implementation = UserSimpleDTO.class)))),
        @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content),
        @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content)
    })
    public ResponseEntity<List<UserSimpleDTO>> findSuggestions(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        log.debug("REST request to list connection suggestions");
        User authenticatedUser = findAuthenticatedUser(userDetails);
        return ResponseEntity.ok(connectionService.findSuggestions(authenticatedUser.getId()));
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
        User authenticatedUser = findAuthenticatedUser(userDetails);
        return ResponseEntity.ok(connectionService.acceptRequest(id, authenticatedUser.getId()));
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
        User authenticatedUser = findAuthenticatedUser(userDetails);
        connectionService.deleteConnection(id, authenticatedUser.getId());
        return ResponseEntity.noContent().build();
    }

    private User findAuthenticatedUser(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
