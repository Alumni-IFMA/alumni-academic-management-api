package com.alumni.academic_management_api.controller;

import com.alumni.academic_management_api.dto.auth.ForgotPasswordRequestDTO;
import com.alumni.academic_management_api.dto.auth.LoginRequestDTO;
import com.alumni.academic_management_api.dto.auth.LoginResponseDTO;
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

    @PostMapping("/forgot-password")
    @Operation(summary = "Solicitar recuperação de senha", description = "Gera um token de recuperação e " +
            "envia para o e-mail do usuário.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Solicitação processada com sucesso",
                    content = @Content),
            @ApiResponse(responseCode = "400", description = "E-mail com formato inválido",
                    content = @Content)
    })
    public ResponseEntity<Void> forgotPasssword(@RequestBody @Valid ForgotPasswordRequestDTO request){
        log.debug("REST request to recover password");
        authService.generatePasswordResetToken(request.getEmail());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Redefinir senha", description = "Valida o token e atualiza a senha do usuário no banco de dados.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Senha redefinida com sucesso",
                    content = @Content),
            @ApiResponse(responseCode = "400", description = "Token inválido, expirado ou formato de requisição incorreto",
                    content = @Content)
    })
    public ResponseEntity<Void> resetPassword(@RequestBody @Valid ResetPasswordRequestDTO request) {
        log.debug("REST request to reset password");
        authService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok().build();
    }

}