package com.alumni.academic_management_api.dto.news;

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
public class NewsResponseDTO implements Serializable {

    private Long id;
    private String title;
    private String summary;
    private String content;
    private String coverImageUrl;
    private LocalDateTime publishedAt;
    private boolean active;
    private LocalDateTime createdAt;
}
