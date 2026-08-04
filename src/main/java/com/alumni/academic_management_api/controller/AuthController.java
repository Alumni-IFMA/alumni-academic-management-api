package com.alumni.academic_management_api.controller;

import com.alumni.academic_management_api.dto.auth.ForgotPasswordRequestDTO;
import com.alumni.academic_management_api.dto.auth.LoginRequestDTO;
import com.alumni.academic_management_api.dto.auth.LoginResponseDTO;
import com.alumni.academic_management_api.dto.auth.RefreshTokenRequestDTO;
import com.alumni.academic_management_api.dto.auth.ResetPasswordRequestDTO;
import com.alumni.academic_management_api.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticação", description = "Operações de login e geração de token JWT")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Autenticar usuário", description = "Valida credenciais e retorna um token JWT.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Autenticado com sucesso",
            content = @Content(schema = @Schema(implementation = LoginResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Credenciais inválidas",
            content = @Content),
        @ApiResponse(responseCode = "400", description = "Corpo da requisição inválido",
            content = @Content)
    })
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid LoginRequestDTO request) {
        log.debug("REST request to authenticate user");
        LoginResponseDTO response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renovar token de acesso", description = "Valida o refresh token, revoga-o e " +
            "retorna um novo par de access/refresh token.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Token renovado com sucesso",
            content = @Content(schema = @Schema(implementation = LoginResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Refresh token inválido, expirado ou revogado",
            content = @Content),
        @ApiResponse(responseCode = "400", description = "Corpo da requisição inválido",
            content = @Content)
    })
    public ResponseEntity<LoginResponseDTO> refresh(@RequestBody @Valid RefreshTokenRequestDTO request) {
        log.debug("REST request to refresh access token");
        LoginResponseDTO response = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Encerrar sessão", description = "Revoga o refresh token informado, impedindo " +
            "que ele seja usado para gerar novos tokens de acesso.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Sessão encerrada com sucesso",
            content = @Content),
        @ApiResponse(responseCode = "400", description = "Corpo da requisição inválido",
            content = @Content)
    })
    public ResponseEntity<Void> logout(@RequestBody @Valid RefreshTokenRequestDTO request) {
        log.debug("REST request to logout");
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Solicitar recuperação de senha", description = "Gera um token de recuperação e " +
            "envia para o email do usuário.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitação processada com sucesso",
                    content = @Content),
            @ApiResponse(responseCode = "400", description = "Email com formato inválido",
                    content = @Content)
    })
    public ResponseEntity<Void> forgotPasssword(@RequestBody @Valid ForgotPasswordRequestDTO request){
        log.debug("REST request to recover password");
        authService.generatePasswordResetToken(request.getEmail());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Redefinir senha", description = "Valida o token e atualiza a senha no banco de dados.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Senha redefinida com sucesso",
                    content = @Content),
            @ApiResponse(responseCode = "400", description = "Formato de requisição incorreto",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Token inválido ou expirado",
                    content = @Content)
    })
    public ResponseEntity<Void> resetPassword(@RequestBody @Valid ResetPasswordRequestDTO request) {
        log.debug("REST request to reset password");
        authService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok().build();
    }

}