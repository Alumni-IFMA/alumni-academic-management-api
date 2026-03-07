package com.alumni.academic_management_api.service;

import com.alumni.academic_management_api.dto.campusescourses.CampusCourseResponseDTO;
import com.alumni.academic_management_api.entity.CampusCourse;
import com.alumni.academic_management_api.mapper.CampusCourseMapper;
import com.alumni.academic_management_api.repository.CampusesCourseRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CampusesCourseService {

    private final CampusesCourseRepository campusesCourseRepository;
    private final CampusCourseMapper campusCourseMapper;

    public CampusesCourseService(
            CampusesCourseRepository campusesCourseRepository,
            CampusCourseMapper campusCourseMapper
    ) {
        this.campusesCourseRepository = campusesCourseRepository;
        this.campusCourseMapper = campusCourseMapper;
    }

    public List<CampusCourseResponseDTO> getALlCampusesCourse() {
        List<CampusCourse> campusCourses = campusesCourseRepository.findAll();
        
        return campusCourseMapper.toDTOList(campusCourses);
    }
}
