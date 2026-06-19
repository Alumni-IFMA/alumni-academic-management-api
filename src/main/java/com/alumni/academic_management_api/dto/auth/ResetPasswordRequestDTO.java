package com.alumni.academic_management_api.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Requisição para redefinir a senha utilizando o token")
public class ResetPasswordRequestDTO implements Serializable {

    @NotBlank(message = "O token é obrigatório")
    @Schema(description = "Token de recuperação recebido por e-mail")
    private String token;

    @NotBlank(message = "A nova senha não pode estar em branco")
    @Size(min = 8, message = "A nova senha deve ter no mínimo 8 caracteres")
    @Schema(description = "A nova senha desejada pelo usuário", format = "password")
    private String newPassword;
}