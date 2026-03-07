package com.alumni.academic_management_api.service;

import com.alumni.academic_management_api.dto.campusescourses.CampusCourseResponseDTO;
import com.alumni.academic_management_api.entity.CampusCourse;
import com.alumni.academic_management_api.mapper.CampusCourseMapper;
import com.alumni.academic_management_api.repository.CampusesCourseRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CampusesCourseServiceTest {
    @Mock
    private CampusesCourseRepository campusesCourseRepository;

    @Mock
    private CampusCourseMapper campusCourseMapper;

    @InjectMocks
    private CampusesCourseService campusesCourseService;

    @Nested
    class getAll {
        @Test
        void givenValidRequest_whenGetAll_thenReturnListOfCampusCourseResponseDTO() {
            CampusCourseResponseDTO campusCourseResponseDTO = CampusCourseResponseDTO.builder()
                    .id(1L)
                    .build();

            CampusCourse campusCourse = CampusCourse.builder()
                    .id(1L)
                    .build();

            List<CampusCourse> campusCourses = List.of(campusCourse);

            Mockito.when(campusesCourseRepository.findAll()).thenReturn(campusCourses);

            Mockito.when(campusCourseMapper.toDTOList(campusCourses)).thenReturn(List.of(campusCourseResponseDTO));

            CampusCourseResponseDTO responseDTO = campusesCourseService.getALlCampusesCourse().get(0);

            assertThat(responseDTO).isEqualTo(campusCourseResponseDTO);
        }

        @Test
        void givenEmptyList_whenGetAll_thenReturnEmptyList() {
            Mockito.when(campusesCourseRepository.findAll()).thenReturn(List.of());

            Mockito.when(campusCourseMapper.toDTOList(List.of())).thenReturn(List.of());

            CampusCourseResponseDTO responseDTO = campusesCourseService.getALlCampusesCourse()
                    .stream().findFirst().orElse(null);

            assertThat(responseDTO).isNull();
        }
    }
}