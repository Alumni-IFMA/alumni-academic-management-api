package com.alumni.academic_management_api.dto.news;

import jakarta.validation.constraints.NotBlank;
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
public class NewsRequestDTO implements Serializable {

    @NotBlank
    private String title;

    private String summary;

    @NotBlank
    private String content;

    private String coverImageUrl;

    private LocalDateTime publishedAt;
}
