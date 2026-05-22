package com.alumni.academic_management_api.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Credenciais para autenticação")
public class LoginRequestDTO implements Serializable {

    @NotBlank
    @Email
    @Schema(description = "E-mail cadastrado do usuário", example = "joao@example.com")
    private String email;

    @NotBlank
    @Schema(description = "Senha do usuário", example = "senhaSegura123")
    private String password;
}
