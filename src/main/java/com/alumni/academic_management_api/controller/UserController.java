package com.alumni.academic_management_api.controller;

import com.alumni.academic_management_api.dto.user.RegisterRequestDTO;
import com.alumni.academic_management_api.dto.user.UpdateRoleRequestDTO;
import com.alumni.academic_management_api.dto.user.UserProfileResponseDTO;
import com.alumni.academic_management_api.dto.user.UserSimpleDTO;
import com.alumni.academic_management_api.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/auth")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserSimpleDTO> createUser(@RequestBody @Valid RegisterRequestDTO request) {
        log.debug("REST request to create user");

        UserSimpleDTO response = userService.createUser(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserSimpleDTO>> findAll() {
        log.debug("REST request to get all users");

        List<UserSimpleDTO> response = userService.findAll();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/users/{id}/profile")
    public ResponseEntity<UserProfileResponseDTO> getUserProfile(@PathVariable Long id) {
        log.debug("REST request to get user profile: {}", id);

        UserProfileResponseDTO response = userService.getUserProfile(id);
      
        return ResponseEntity.ok(response);
    }
  
    @GetMapping("/users/{id}")
    public ResponseEntity<UserSimpleDTO> getUserById(@PathVariable Long id) {
        log.debug("REST request to get user by id: {}", id);

        UserSimpleDTO response = userService.findUserById(id);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/users/{id}/role")
    public ResponseEntity<UserSimpleDTO> updateUserRole(
            @PathVariable Long id,
            @RequestBody @Valid UpdateRoleRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.debug("REST request to update role of user: {}", id);
        UserSimpleDTO response = userService.updateUserRole(id, request.getRole(), userDetails.getUsername());
        return ResponseEntity.ok(response);
    }
}
