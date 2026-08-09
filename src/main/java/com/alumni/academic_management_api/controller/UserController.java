package com.alumni.academic_management_api.controller;

import com.alumni.academic_management_api.config.OpenApiConfig;
import com.alumni.academic_management_api.dto.user.RegisterRequestDTO;
import com.alumni.academic_management_api.dto.user.UpdateRoleRequestDTO;
import com.alumni.academic_management_api.dto.user.UserProfileResponseDTO;
import com.alumni.academic_management_api.dto.user.UserSimpleDTO;
import com.alumni.academic_management_api.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth")
@Tag(name = "Usuários", description = "Cadastro, consulta e gerenciamento de usuários")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    @Operation(summary = "Cadastrar novo usuário",
        description = "Cria um usuário com perfil acadêmico inicial. Não requer autenticação.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Usuário criado com sucesso",
            content = @Content(schema = @Schema(implementation = UserSimpleDTO.class))),
        @ApiResponse(responseCode = "400", description = "Dados inválidos ou CPF/e-mail já cadastrado",
            content = @Content)
    })
    public ResponseEntity<UserSimpleDTO> createUser(@RequestBody @Valid RegisterRequestDTO request) {
        log.debug("REST request to create user");
        UserSimpleDTO response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/users")
    @Operation(summary = "Listar todos os usuários", description = "Acesso restrito a administradores.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso",
            content = @Content(array = @ArraySchema(
                    schema = @Schema(implementation = UserSimpleDTO.class)))),
        @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content),
        @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content)
    })
    public ResponseEntity<List<UserSimpleDTO>> findAll() {
        log.debug("REST request to get all users");
        List<UserSimpleDTO> response = userService.findAll();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "Buscar usuário por ID")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuário encontrado",
            content = @Content(schema = @Schema(implementation = UserSimpleDTO.class))),
        @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content),
        @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content)
    })
    public ResponseEntity<UserSimpleDTO> getUserById(
            @Parameter(description = "ID do usuário") @PathVariable Long id) {
        log.debug("REST request to get user by id: {}", id);
        UserSimpleDTO response = userService.findUserById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users/{id}/profile")
    @Operation(summary = "Buscar perfil completo do usuário",
        description = "Retorna dados pessoais, profissionais e perfis acadêmicos.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Perfil retornado com sucesso",
            content = @Content(schema = @Schema(implementation = UserProfileResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content),
        @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content)
    })
    public ResponseEntity<UserProfileResponseDTO> getUserProfile(
            @Parameter(description = "ID do usuário") @PathVariable Long id) {
        log.debug("REST request to get user profile: {}", id);
        UserProfileResponseDTO response = userService.getUserProfile(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/users/{id}/profile-picture")
    @Operation(summary = "Upload de foto de perfil",
        description = "Envia imagem para o MinIO e atualiza a URL no perfil do usuário.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "URL da imagem retornada",
            content = @Content(schema = @Schema(example = "{\"profilePictureUrl\": \"https://...\"}"))),
        @ApiResponse(responseCode = "400", description = "Arquivo inválido", content = @Content),
        @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content),
        @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content)
    })
    public ResponseEntity<Map<String, String>> uploadProfilePicture(
            @Parameter(description = "ID do usuário") @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        log.debug("REST request to upload profile picture for user: {}", id);
        String url = userService.uploadProfilePicture(id, file);
        return ResponseEntity.ok(Map.of("profilePictureUrl", url));
    }

    @PatchMapping("/users/{id}/role")
    @Operation(summary = "Atualizar papel do usuário", description = "Acesso restrito a administradores.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Papel atualizado com sucesso",
            content = @Content(schema = @Schema(implementation = UserSimpleDTO.class))),
        @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content),
        @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content),
        @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content)
    })
    public ResponseEntity<UserSimpleDTO> updateUserRole(
            @Parameter(description = "ID do usuário") @PathVariable Long id,
            @RequestBody @Valid UpdateRoleRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.debug("REST request to update role of user: {}", id);
        UserSimpleDTO response = userService.updateUserRole(id, request.getRole(), userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/users/{id}/approve")
    @Operation(summary = "Aprovar cadastro de usuário", description = "Acesso restrito a administradores.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuário aprovado com sucesso",
            content = @Content(schema = @Schema(implementation = UserSimpleDTO.class))),
        @ApiResponse(responseCode = "400", description = "Conta não está pendente de verificação",
            content = @Content),
        @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content),
        @ApiResponse(responseCode = "403", description = "Acesso negado", content = @Content),
        @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content)
    })
    public ResponseEntity<UserSimpleDTO> approveUser(
            @Parameter(description = "ID do usuário") @PathVariable Long id) {
        log.debug("REST request to approve user: {}", id);
        UserSimpleDTO response = userService.approveUser(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/users/{id}/onboarding")
    @Operation(summary = "Concluir onboarding", description = "Atualiza a flag has_seen_tutorial para true.")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Tutorial marcado como visto com sucesso"),
            @ApiResponse(responseCode = "400", description = "Sem permissão para alterar o status", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content)
    })
    public ResponseEntity<Void> completeOnboarding(
            @Parameter(description = "ID do usuário") @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.debug("REST request to complete onboarding for user: {}", id);
        userService.completeOnboarding(id, userDetails.getUsername());

        return ResponseEntity.noContent().build();
    }
}
