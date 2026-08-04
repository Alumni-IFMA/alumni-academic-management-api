package com.alumni.academic_management_api.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Token JWT gerado após autenticação bem-sucedida")
public class LoginResponseDTO implements Serializable {

    @Schema(description = "Token Bearer JWT", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;

    @Schema(description = "Token opaco usado para obter um novo token JWT sem reautenticação",
            example = "8f14e45fceea167a5a36dedd4bea2543...")
    private String refreshToken;

    @Schema(description = "Identificador do usuário autenticado", example = "42")
    private Long id;
}
