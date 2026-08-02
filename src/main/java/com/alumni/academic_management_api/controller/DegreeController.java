package com.alumni.academic_management_api.controller;

import com.alumni.academic_management_api.dto.degree.DegreeRequestDTO;
import com.alumni.academic_management_api.dto.degree.DegreeResponseDTO;
import com.alumni.academic_management_api.service.DegreeService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/degrees")
public class DegreeController {

    private final DegreeService degreeService;

    public DegreeController(DegreeService degreeService) {
        this.degreeService = degreeService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DegreeResponseDTO> upload(@ModelAttribute @Valid DegreeRequestDTO request) {
        log.debug("REST request to upload degree for user: {}", request.getUserId());

        return ResponseEntity.status(HttpStatus.CREATED).body(degreeService.upload(request));
    }

    @GetMapping("/me")
    public ResponseEntity<List<DegreeResponseDTO>> findMine(@AuthenticationPrincipal UserDetails userDetails) {
        log.debug("REST request to list degrees for authenticated user");

        return ResponseEntity.ok(degreeService.findByAuthenticatedUser(userDetails.getUsername()));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Map<String, String>> download(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.debug("REST request to download degree: {}", id);

        String url = degreeService.generateDownloadUrl(id, userDetails.getUsername());

        return ResponseEntity.ok(Map.of("downloadUrl", url));
    }
}
