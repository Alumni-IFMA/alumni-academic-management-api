package com.alumni.academic_management_api.controller;

import com.alumni.academic_management_api.dto.job.JobRequestDTO;
import com.alumni.academic_management_api.dto.job.JobResponseDTO;
import com.alumni.academic_management_api.enums.ExperienceLevel;
import com.alumni.academic_management_api.service.JobService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    public ResponseEntity<JobResponseDTO> create(@RequestBody @Valid JobRequestDTO request) {
        log.debug("REST request to create job");

        return ResponseEntity.status(HttpStatus.CREATED).body(jobService.create(request));
    }

    @GetMapping
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
    public ResponseEntity<JobResponseDTO> findById(@PathVariable Long id) {
        log.debug("REST request to get job: {}", id);

        return ResponseEntity.ok(jobService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<JobResponseDTO> update(
            @PathVariable Long id,
            @RequestBody @Valid JobRequestDTO request
    ) {
        log.debug("REST request to update job: {}", id);

        return ResponseEntity.ok(jobService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.debug("REST request to deactivate job: {}", id);

        jobService.delete(id);

        return ResponseEntity.noContent().build();
    }
}
