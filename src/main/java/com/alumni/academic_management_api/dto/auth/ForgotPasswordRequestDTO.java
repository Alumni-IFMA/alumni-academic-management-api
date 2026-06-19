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
@Schema(description = "Requisição para solicitar a recuperação de senha")
public class ForgotPasswordRequestDTO implements Serializable {

    @NotBlank(message = "O e-mail não pode estar em branco")
    @Email(message = "Formato de e-mail inválido")
    @Schema(description = "E-mail cadastrado do usuário para receber o link de recuperação", example = "estudante@alumni.ifma.edu.br")
    private String email;
}