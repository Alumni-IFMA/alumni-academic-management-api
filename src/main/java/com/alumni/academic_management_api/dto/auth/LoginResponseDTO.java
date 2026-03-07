package com.alumni.academic_management_api.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsCosntructor;

import java.io.Serializable;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponseDTO implements Serializable{
    private String token;
}