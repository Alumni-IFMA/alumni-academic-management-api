package com.alumni.academic_management_api.controller;

import com.alumni.academic_management_api.config.OpenApiConfig;
import com.alumni.academic_management_api.dto.job.JobRequestDTO;
import com.alumni.academic_management_api.dto.job.JobResponseDTO;
import com.alumni.academic_management_api.enums.ExperienceLevel;
import com.alumni.academic_management_api.service.JobService;
import com.alumni.academic_management_api.service.SavedJobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/jobs")
@Tag(name = "Vagas", description = "Publicação, consulta e gerenciamento de vagas de emprego")
public class JobController {

    private final JobService jobService;
    private final SavedJobService savedJobService;

    public JobController(JobService jobService, SavedJobService savedJobService) {
        this.jobService = jobService;
        this.savedJobService = savedJobService;
    }

    @PostMapping
    @Operation(
            summary = "Cadastrar vaga",
            description = "Cria uma nova vaga de emprego. Requer perfil ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Vaga criada com sucesso",
                    content = @Content(schema = @Schema(implementation = JobResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sem permissão de ADMIN", content = @Content)
    })
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    public ResponseEntity<JobResponseDTO> create(@RequestBody @Valid JobRequestDTO request) {
        log.debug("REST request to create job");

        return ResponseEntity.status(HttpStatus.CREATED).body(jobService.create(request));
    }

    @GetMapping
    @Operation(
            summary = "Listar vagas",
            description = "Retorna uma página de vagas ativas, com filtros opcionais por palavra-chave, área, "
                    + "nível de experiência, localização, salário mínimo e modalidade remota."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de vagas retornada com sucesso")
    })
    public ResponseEntity<Page<JobResponseDTO>> findAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) List<ExperienceLevel> experience,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) BigDecimal minSalary,
            @RequestParam(required = false) Boolean remote,
            Pageable pageable
    ) {
        log.debug("REST request to list jobs");

        return ResponseEntity.ok(
                jobService.findAll(keyword, area, experience, location, minSalary, remote, pageable)
        );
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Buscar vaga por ID",
            description = "Retorna os detalhes de uma vaga específica."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vaga encontrada",
                    content = @Content(schema = @Schema(implementation = JobResponseDTO.class))),
            @ApiResponse(responseCode = "404", description = "Vaga não encontrada", content = @Content)
    })
    public ResponseEntity<JobResponseDTO> findById(@PathVariable Long id) {
        log.debug("REST request to get job: {}", id);

        return ResponseEntity.ok(jobService.findById(id));
    }

    @GetMapping("/saved")
    @Operation(
            summary = "Listar vagas salvas",
            description = "Retorna a lista de vagas salvas pelo usuário autenticado, da mais recente para a mais "
                    + "antiga."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de vagas salvas retornada com sucesso",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = JobResponseDTO.class)))),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content)
    })
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    public ResponseEntity<List<JobResponseDTO>> findSavedJobs(@AuthenticationPrincipal UserDetails userDetails) {
        log.debug("REST request to list saved jobs");

        return ResponseEntity.ok(savedJobService.findSavedJobs(userDetails.getUsername()));
    }

    @PostMapping("/{id}/save")
    @Operation(
            summary = "Salvar vaga",
            description = "Salva uma vaga para o usuário autenticado, permitindo consultá-la depois na lista de "
                    + "vagas salvas."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Vaga salva com sucesso", content = @Content),
            @ApiResponse(responseCode = "400", description = "Vaga já foi salva anteriormente", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content),
            @ApiResponse(responseCode = "404", description = "Vaga não encontrada", content = @Content)
    })
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    public ResponseEntity<Void> saveJob(
            @Parameter(description = "ID da vaga") @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        log.debug("REST request to save job: {}", id);

        savedJobService.saveJob(userDetails.getUsername(), id);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{id}/save")
    @Operation(
            summary = "Remover vaga salva",
            description = "Remove uma vaga da lista de vagas salvas do usuário autenticado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Vaga removida da lista de salvas", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content),
            @ApiResponse(responseCode = "404", description = "Vaga não estava salva", content = @Content)
    })
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    public ResponseEntity<Void> unsaveJob(
            @Parameter(description = "ID da vaga") @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        log.debug("REST request to unsave job: {}", id);

        savedJobService.unsaveJob(userDetails.getUsername(), id);

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Atualizar vaga",
            description = "Atualiza os dados de uma vaga existente. Requer perfil ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vaga atualizada com sucesso",
                    content = @Content(schema = @Schema(implementation = JobResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sem permissão de ADMIN", content = @Content),
            @ApiResponse(responseCode = "404", description = "Vaga não encontrada", content = @Content)
    })
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    public ResponseEntity<JobResponseDTO> update(
            @PathVariable Long id,
            @RequestBody @Valid JobRequestDTO request
    ) {
        log.debug("REST request to update job: {}", id);

        return ResponseEntity.ok(jobService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Desativar vaga",
            description = "Desativa uma vaga, removendo-a das listagens públicas. Requer perfil ADMIN."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Vaga desativada com sucesso", content = @Content),
            @ApiResponse(responseCode = "401", description = "Não autenticado", content = @Content),
            @ApiResponse(responseCode = "403", description = "Sem permissão de ADMIN", content = @Content),
            @ApiResponse(responseCode = "404", description = "Vaga não encontrada", content = @Content)
    })
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.debug("REST request to deactivate job: {}", id);

        jobService.delete(id);

        return ResponseEntity.noContent().build();
    }
}
