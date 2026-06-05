package com.alumni.academic_management_api.dto.campusescourses;

import com.alumni.academic_management_api.enums.Level;
import com.alumni.academic_management_api.enums.Modality;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Vínculo entre um campus e um curso disponível para cadastro")
public class CampusCourseResponseDTO implements Serializable {

    @Schema(description = "ID do vínculo campus-curso", example = "3")
    Long id;

    @Schema(description = "Nome do campus", example = "Campus Florianópolis")
    String campusName;

    @Schema(description = "Nome do curso", example = "Análise e Desenvolvimento de Sistemas")
    String courseName;

    @Schema(description = "Nível do curso: GRADUATION, POSTGRADUATION, TECHNICAL")
    Level level;

    @Schema(description = "Modalidade: PRESENTIAL, HYBRID, REMOTE")
    Modality modality;
}
