package com.alumni.academic_management_api.dto.user;

import com.alumni.academic_management_api.enums.Level;
import com.alumni.academic_management_api.enums.Modality;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Perfil acadêmico de um usuário em um curso específico")
public class AcademicProfileResponseDTO implements Serializable {

    @Schema(description = "ID do perfil acadêmico", example = "5")
    Long id;

    @Schema(description = "Ano de ingresso no curso", example = "2018")
    Integer entryYear;

    @Schema(description = "Ano de conclusão do curso", example = "2022")
    Integer conclusionYear;

    @Schema(description = "Nome do campus", example = "Campus Central")
    String campusName;

    @Schema(description = "Nome do curso", example = "Ciência da Computação")
    String courseName;

    @Schema(description = "Nível do curso: GRADUATION, POSTGRADUATION, TECHNICAL")
    Level level;

    @Schema(description = "Modalidade: PRESENTIAL, HYBRID, REMOTE")
    Modality modality;
}
