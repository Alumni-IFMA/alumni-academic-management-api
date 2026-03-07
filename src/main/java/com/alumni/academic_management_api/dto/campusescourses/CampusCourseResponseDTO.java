package com.alumni.academic_management_api.dto.campusescourses;

import com.alumni.academic_management_api.enums.Level;
import com.alumni.academic_management_api.enums.Modality;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampusCourseResponseDTO implements Serializable {

    Long id;

    String campusName;

    String courseName;

    Level level;

    Modality modality;
}
