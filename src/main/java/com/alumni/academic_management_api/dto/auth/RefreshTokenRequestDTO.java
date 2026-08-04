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
@Schema(description = "Requisição contendo o refresh token")
public class RefreshTokenRequestDTO implements Serializable {

    @NotBlank(message = "O refresh token é obrigatório")
    @Schema(description = "Refresh token recebido no login")
    private String refreshToken;
}
