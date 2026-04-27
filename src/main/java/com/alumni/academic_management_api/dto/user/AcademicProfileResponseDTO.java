package com.alumni.academic_management_api.dto.user;

import com.alumni.academic_management_api.enums.Level;
import com.alumni.academic_management_api.enums.Modality;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AcademicProfileResponseDTO implements Serializable {

    Long id;

    Integer entryYear;

    Integer conclusionYear;

    String campusName;

    String courseName;

    Level level;

    Modality modality;
}
