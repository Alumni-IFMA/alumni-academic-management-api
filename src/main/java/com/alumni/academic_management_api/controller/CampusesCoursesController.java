package com.alumni.academic_management_api.controller;

import com.alumni.academic_management_api.dto.campusescourses.CampusCourseResponseDTO;
import com.alumni.academic_management_api.service.CampusesCourseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping
public class CampusesCoursesController {
    private final CampusesCourseService campusesCourseService;

    public CampusesCoursesController(CampusesCourseService campusesCourseService) {
        this.campusesCourseService = campusesCourseService;
    }

    @GetMapping("/campus-courses")
    public ResponseEntity<List<CampusCourseResponseDTO>> getAll() {
        log.debug("REST request to get all campuses and courses");

        List<CampusCourseResponseDTO> response = campusesCourseService.getAllCampusesCourse();

        return ResponseEntity.ok(response);
    }
}
