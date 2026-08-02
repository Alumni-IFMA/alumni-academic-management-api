package com.alumni.academic_management_api.dto.degree;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DegreeRequestDTO {

    @NotNull
    private Long userId;

    @NotBlank
    private String title;

    private MultipartFile file;
}
