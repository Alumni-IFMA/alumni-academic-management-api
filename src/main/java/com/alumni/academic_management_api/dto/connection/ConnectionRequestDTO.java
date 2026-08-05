package com.alumni.academic_management_api.dto.connection;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Dados para enviar uma solicitação de conexão")
public class ConnectionRequestDTO implements Serializable {

    @NotNull
    @Schema(description = "ID do usuário que receberá a solicitação", example = "2")
    private Long addresseeId;
}
