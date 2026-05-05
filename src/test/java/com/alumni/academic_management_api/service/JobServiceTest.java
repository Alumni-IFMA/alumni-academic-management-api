package com.alumni.academic_management_api.service;

import com.alumni.academic_management_api.dto.job.JobRequestDTO;
import com.alumni.academic_management_api.dto.job.JobResponseDTO;
import com.alumni.academic_management_api.entity.Job;
import com.alumni.academic_management_api.enums.ExperienceLevel;
import com.alumni.academic_management_api.enums.WorkplaceType;
import com.alumni.academic_management_api.exception.ResourceNotFoundException;
import com.alumni.academic_management_api.mapper.JobMapper;
import com.alumni.academic_management_api.repository.JobRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobMapper jobMapper;

    @InjectMocks
    private JobService jobService;

    private static Job buildActiveJob() {
        return Job.builder()
                .id(1L)
                .title("Desenvolvedor Java")
                .company("Empresa X")
                .companyLogoUrl("https://empresa.com/logo.png")
                .description("Descrição da vaga")
                .location("São Paulo")
                .area("Tecnologia")
                .workplaceType(WorkplaceType.HYBRID)
                .experienceLevel(ExperienceLevel.MID)
                .salary(new BigDecimal("8000.00"))
                .externalLink("https://empresa.com/vaga")
                .requirements(List.of("Java 17+", "Spring Boot"))
                .benefits(List.of("Plano de Saúde", "VA/VR"))
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private static JobRequestDTO buildRequestDTO() {
        return JobRequestDTO.builder()
                .title("Desenvolvedor Java")
                .company("Empresa X")
                .companyLogoUrl("https://empresa.com/logo.png")
                .description("Descrição da vaga")
                .location("São Paulo")
                .area("Tecnologia")
                .workplaceType(WorkplaceType.HYBRID)
                .experienceLevel(ExperienceLevel.MID)
                .salary(new BigDecimal("8000.00"))
                .externalLink("https://empresa.com/vaga")
                .requirements(List.of("Java 17+", "Spring Boot"))
                .benefits(List.of("Plano de Saúde", "VA/VR"))
                .build();
    }

    private static JobResponseDTO buildResponseDTO() {
        return JobResponseDTO.builder()
                .id(1L)
                .title("Desenvolvedor Java")
                .company("Empresa X")
                .companyLogoUrl("https://empresa.com/logo.png")
                .description("Descrição da vaga")
                .location("São Paulo")
                .area("Tecnologia")
                .workplaceType(WorkplaceType.HYBRID)
                .experienceLevel(ExperienceLevel.MID)
                .salary(new BigDecimal("8000.00"))
                .externalLink("https://empresa.com/vaga")
                .requirements(List.of("Java 17+", "Spring Boot"))
                .benefits(List.of("Plano de Saúde", "VA/VR"))
                .active(true)
                .build();
    }

    @Nested
    class Create {

        @Test
        void givenValidRequest_whenCreate_thenReturnJobResponseDTO() {
            JobRequestDTO request = buildRequestDTO();
            Job job = buildActiveJob();
            JobResponseDTO response = buildResponseDTO();

            Mockito.when(jobMapper.toEntity(request)).thenReturn(job);
            Mockito.when(jobRepository.save(job)).thenReturn(job);
            Mockito.when(jobMapper.toResponseDTO(job)).thenReturn(response);

            JobResponseDTO result = jobService.create(request);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getTitle()).isEqualTo("Desenvolvedor Java");
            assertThat(result.isActive()).isTrue();
            assertThat(result.getRequirements()).containsExactly("Java 17+", "Spring Boot");
            assertThat(result.getBenefits()).containsExactly("Plano de Saúde", "VA/VR");
            assertThat(result.getCompanyLogoUrl()).isEqualTo("https://empresa.com/logo.png");

            Mockito.verify(jobMapper).toEntity(request);
            Mockito.verify(jobRepository).save(job);
            Mockito.verify(jobMapper).toResponseDTO(job);
        }
    }

    @Nested
    class FindById {

        @Test
        void givenExistingActiveJob_whenFindById_thenReturnJobResponseDTO() {
            Job job = buildActiveJob();
            JobResponseDTO response = buildResponseDTO();

            Mockito.when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
            Mockito.when(jobMapper.toResponseDTO(job)).thenReturn(response);

            JobResponseDTO result = jobService.findById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);

            Mockito.verify(jobRepository).findById(1L);
            Mockito.verify(jobMapper).toResponseDTO(job);
        }

        @Test
        void givenNonExistingJob_whenFindById_thenThrowResourceNotFoundException() {
            Mockito.when(jobRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> jobService.findById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");

            Mockito.verify(jobMapper, Mockito.never()).toResponseDTO(Mockito.any());
        }

        @Test
        void givenInactiveJob_whenFindById_thenThrowResourceNotFoundException() {
            Job inactiveJob = Job.builder()
                    .id(1L)
                    .title("Vaga Inativa")
                    .company("Empresa X")
                    .description("Descrição")
                    .active(false)
                    .build();

            Mockito.when(jobRepository.findById(1L)).thenReturn(Optional.of(inactiveJob));

            assertThatThrownBy(() -> jobService.findById(1L))
                    .isInstanceOf(ResourceNotFoundException.class);

            Mockito.verify(jobMapper, Mockito.never()).toResponseDTO(Mockito.any());
        }
    }

    @Nested
    class FindAll {

        @Test
        @SuppressWarnings("unchecked")
        void givenNoFilters_whenFindAll_thenReturnPage() {
            Job job = buildActiveJob();
            JobResponseDTO response = buildResponseDTO();
            Pageable pageable = PageRequest.of(0, 10);
            Page<Job> jobPage = new PageImpl<>(List.of(job));

            Mockito.when(jobRepository.findAll(Mockito.any(Specification.class), Mockito.eq(pageable)))
                    .thenReturn(jobPage);
            Mockito.when(jobMapper.toResponseDTO(job)).thenReturn(response);

            Page<JobResponseDTO> result =
                    jobService.findAll(null, null, null, null, null, null, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("Desenvolvedor Java");
        }

        @Test
        @SuppressWarnings("unchecked")
        void givenRemoteFilter_whenFindAll_thenReturnFilteredPage() {
            Job remoteJob = Job.builder()
                    .id(2L)
                    .title("Dev Remoto")
                    .company("Tech Co")
                    .description("Vaga remota")
                    .workplaceType(WorkplaceType.REMOTE)
                    .active(true)
                    .build();
            JobResponseDTO remoteResponse = JobResponseDTO.builder()
                    .id(2L)
                    .title("Dev Remoto")
                    .workplaceType(WorkplaceType.REMOTE)
                    .active(true)
                    .build();
            Pageable pageable = PageRequest.of(0, 10);
            Page<Job> jobPage = new PageImpl<>(List.of(remoteJob));

            Mockito.when(jobRepository.findAll(Mockito.any(Specification.class), Mockito.eq(pageable)))
                    .thenReturn(jobPage);
            Mockito.when(jobMapper.toResponseDTO(remoteJob)).thenReturn(remoteResponse);

            Page<JobResponseDTO> result =
                    jobService.findAll(null, null, null, null, null, true, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getWorkplaceType()).isEqualTo(WorkplaceType.REMOTE);
        }

        @Test
        @SuppressWarnings("unchecked")
        void givenMultipleExperienceLevels_whenFindAll_thenReturnFilteredPage() {
            Job juniorJob = Job.builder()
                    .id(3L)
                    .title("Dev Junior")
                    .company("Startup")
                    .description("Vaga junior")
                    .experienceLevel(ExperienceLevel.JUNIOR)
                    .active(true)
                    .build();
            JobResponseDTO juniorResponse = JobResponseDTO.builder()
                    .id(3L)
                    .title("Dev Junior")
                    .experienceLevel(ExperienceLevel.JUNIOR)
                    .active(true)
                    .build();
            Pageable pageable = PageRequest.of(0, 10);
            Page<Job> jobPage = new PageImpl<>(List.of(juniorJob));

            Mockito.when(jobRepository.findAll(Mockito.any(Specification.class), Mockito.eq(pageable)))
                    .thenReturn(jobPage);
            Mockito.when(jobMapper.toResponseDTO(juniorJob)).thenReturn(juniorResponse);

            Page<JobResponseDTO> result = jobService.findAll(
                    null, null, List.of(ExperienceLevel.JUNIOR, ExperienceLevel.MID),
                    null, null, null, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getExperienceLevel()).isEqualTo(ExperienceLevel.JUNIOR);
        }
    }

    @Nested
    class Update {

        @Test
        void givenValidRequest_whenUpdate_thenReturnUpdatedJobResponseDTO() {
            Job job = buildActiveJob();
            JobRequestDTO request = buildRequestDTO();
            JobResponseDTO response = buildResponseDTO();

            Mockito.when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
            Mockito.doNothing().when(jobMapper).updateEntityFromDTO(request, job);
            Mockito.when(jobRepository.save(job)).thenReturn(job);
            Mockito.when(jobMapper.toResponseDTO(job)).thenReturn(response);

            JobResponseDTO result = jobService.update(1L, request);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);

            Mockito.verify(jobMapper).updateEntityFromDTO(request, job);
            Mockito.verify(jobRepository).save(job);
        }

        @Test
        void givenNonExistingJob_whenUpdate_thenThrowResourceNotFoundException() {
            Mockito.when(jobRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> jobService.update(99L, buildRequestDTO()))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");

            Mockito.verify(jobRepository, Mockito.never()).save(Mockito.any());
        }

        @Test
        void givenInactiveJob_whenUpdate_thenThrowResourceNotFoundException() {
            Job inactiveJob = Job.builder()
                    .id(1L)
                    .title("Vaga Inativa")
                    .company("Empresa X")
                    .description("Descrição")
                    .active(false)
                    .build();

            Mockito.when(jobRepository.findById(1L)).thenReturn(Optional.of(inactiveJob));

            assertThatThrownBy(() -> jobService.update(1L, buildRequestDTO()))
                    .isInstanceOf(ResourceNotFoundException.class);

            Mockito.verify(jobRepository, Mockito.never()).save(Mockito.any());
        }
    }

    @Nested
    class Delete {

        @Test
        void givenExistingJob_whenDelete_thenSetActiveToFalse() {
            Job job = buildActiveJob();

            Mockito.when(jobRepository.findById(1L)).thenReturn(Optional.of(job));

            jobService.delete(1L);

            assertThat(job.isActive()).isFalse();
            Mockito.verify(jobRepository).save(job);
        }

        @Test
        void givenNonExistingJob_whenDelete_thenThrowResourceNotFoundException() {
            Mockito.when(jobRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> jobService.delete(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");

            Mockito.verify(jobRepository, Mockito.never()).save(Mockito.any());
        }
    }
}