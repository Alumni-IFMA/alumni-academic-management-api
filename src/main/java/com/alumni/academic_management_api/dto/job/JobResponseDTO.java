package com.alumni.academic_management_api.dto.job;

import com.alumni.academic_management_api.enums.ExperienceLevel;
import com.alumni.academic_management_api.enums.WorkplaceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class JobResponseDTO implements Serializable {

    private Long id;
    private String title;
    private String company;
    private String companyLogoUrl;
    private String description;
    private String location;
    private String area;
    private WorkplaceType workplaceType;
    private ExperienceLevel experienceLevel;
    private BigDecimal salary;
    private String externalLink;

    @Builder.Default
    private List<String> requirements = new ArrayList<>();

    @Builder.Default
    private List<String> benefits = new ArrayList<>();

    private boolean active;
    private LocalDateTime createdAt;
}