package com.alumni.academic_management_api.controller;

import com.alumni.academic_management_api.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// TEMPORARY — remove before merging to develop
@RestController
@RequestMapping("/dev/email")
public class DevEmailController {

    private final EmailService emailService;

    public DevEmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("/approval")
    public ResponseEntity<String> testApproval(
            @RequestParam String to,
            @RequestParam String name
    ) {
        emailService.sendApprovalEmail(to, name);
        return ResponseEntity.ok("E-mail de aprovação enfileirado para: " + to);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> testForgotPassword(
            @RequestParam String to,
            @RequestParam String token
    ) {
        emailService.sendForgotPasswordEmail(to, token);
        return ResponseEntity.ok("E-mail de recuperação enfileirado para: " + to);
    }
}
