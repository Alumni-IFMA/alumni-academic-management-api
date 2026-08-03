package com.alumni.academic_management_api.dto.connection;

import com.alumni.academic_management_api.dto.user.UserSimpleDTO;
import com.alumni.academic_management_api.enums.ConnectionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Dados de uma conexão entre usuários")
public class ConnectionResponseDTO implements Serializable {

    @Schema(description = "ID único da conexão", example = "1")
    private Long id;

    @Schema(description = "Usuário que enviou a solicitação")
    private UserSimpleDTO requester;

    @Schema(description = "Usuário que recebeu a solicitação")
    private UserSimpleDTO addressee;

    @Schema(description = "Status da conexão: PENDING ou ACCEPTED")
    private ConnectionStatus status;

    @Schema(description = "Data de criação")
    private LocalDateTime createdAt;

    @Schema(description = "Data da última atualização")
    private LocalDateTime updatedAt;
}
