package com.alumni.academic_management_api.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Resposta simples com uma mensagem informativa")
public class MessageResponseDTO implements Serializable {

    @Schema(description = "Mensagem descritiva do resultado da operação")
    private String message;
}
