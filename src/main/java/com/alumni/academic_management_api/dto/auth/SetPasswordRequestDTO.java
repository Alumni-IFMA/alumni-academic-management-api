package com.alumni.academic_management_api.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Requisição para definir a senha pela primeira vez usando o token recebido por e-mail")
public class SetPasswordRequestDTO implements Serializable {

    @NotBlank(message = "O token é obrigatório")
    @Schema(description = "Token de definição de senha recebido por e-mail")
    private String token;

    @NotBlank(message = "A senha não pode estar em branco")
    @Schema(description = "A senha desejada pelo usuário", format = "password")
    private String password;

    @NotBlank(message = "A confirmação de senha não pode estar em branco")
    @Schema(description = "Confirmação da senha desejada pelo usuário", format = "password")
    private String passwordConfirmation;
}
