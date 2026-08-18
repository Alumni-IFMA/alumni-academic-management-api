package com.alumni.academic_management_api.dto.support;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SupportMessageRequestDTO implements Serializable {

    @NotBlank
    private String subject;

    @NotBlank
    private String message;
}
