package com.alumini.academic_management_api.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.serializable;

@Getter
@Setter
@AllArgsConstructor;
@noArgsConstructor
public clas LoginRequestDTO implements Serializable {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;
}