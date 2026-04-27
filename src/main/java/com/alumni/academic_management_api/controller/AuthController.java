package com.alumni.academic_management_api.controller;

import com.alumni.academic_management_api.dto.auth.LoginRequestDTO;
import com.alumni.academic_management_api.dto.auth.LoginResponseDTO;
import com.alumni.academic_management_api.service.AuthService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody @Valid LoginRequestDTO request) {
        log.debug("REST request to authenticate user");

        LoginResponseDTO response = authService.login(request);

        return ResponseEntity.ok(response);
    }
}
