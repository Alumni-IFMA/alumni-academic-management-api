package com.alumni.academic_management_api.dto.degree;

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
public class DegreeResponseDTO implements Serializable {

    private Long id;
    private String title;
    private Long userId;
    private String fileUrl;
}
