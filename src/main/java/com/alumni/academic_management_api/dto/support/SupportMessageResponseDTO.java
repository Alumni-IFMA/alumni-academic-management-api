package com.alumni.academic_management_api.dto.support;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SupportMessageResponseDTO implements Serializable {

    private Long id;
    private String name;
    private String email;
    private String subject;
    private String message;
    private boolean resolved;
    private LocalDateTime createdAt;
}
