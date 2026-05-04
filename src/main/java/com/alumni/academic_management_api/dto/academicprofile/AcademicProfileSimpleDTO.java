package com.alumni.academic_management_api.dto.academicprofile;

import com.alumni.academic_management_api.dto.campusescourses.CampusCourseResponseDTO;
import com.alumni.academic_management_api.entity.CampusCourse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AcademicProfileSimpleDTO implements Serializable {

    Long id;

    Integer entryYear;

    Integer conclusionYear;

    CampusCourseResponseDTO campusCourse;

}
