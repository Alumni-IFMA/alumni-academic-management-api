package com.alumni.academic_management_api.controller;

import com.alumni.academic_management_api.dto.campusescourses.CampusCourseResponseDTO;
import com.alumni.academic_management_api.service.CampusesCourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping
@Tag(name = "Campus e Cursos", description = "Consulta de campus e cursos disponíveis para cadastro")
public class CampusesCoursesController {

    private final CampusesCourseService campusesCourseService;

    public CampusesCoursesController(CampusesCourseService campusesCourseService) {
        this.campusesCourseService = campusesCourseService;
    }

    @GetMapping("/campus-courses")
    @Operation(summary = "Listar campus e cursos",
        description = "Retorna todos os vínculos campus-curso disponíveis. Público, sem autenticação.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista retornada com sucesso",
            content = @Content(schema = @Schema(implementation = CampusCourseResponseDTO.class)))
    })
    public ResponseEntity<List<CampusCourseResponseDTO>> getAll() {
        log.debug("REST request to get all campuses and courses");
        List<CampusCourseResponseDTO> response = campusesCourseService.getAllCampusesCourse();
        return ResponseEntity.ok(response);
    }
}
