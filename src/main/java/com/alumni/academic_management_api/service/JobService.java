package com.alumni.academic_management_api.service;

import com.alumni.academic_management_api.dto.job.JobRequestDTO;
import com.alumni.academic_management_api.dto.job.JobResponseDTO;
import com.alumni.academic_management_api.entity.Job;
import com.alumni.academic_management_api.enums.ExperienceLevel;
import com.alumni.academic_management_api.exception.ResourceNotFoundException;
import com.alumni.academic_management_api.mapper.JobMapper;
import com.alumni.academic_management_api.repository.JobRepository;
import com.alumni.academic_management_api.repository.specification.JobSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Transactional
@Service
public class JobService {

    private final JobRepository jobRepository;
    private final JobMapper jobMapper;

    public JobService(JobRepository jobRepository, JobMapper jobMapper) {
        this.jobRepository = jobRepository;
        this.jobMapper = jobMapper;
    }

    public JobResponseDTO create(JobRequestDTO dto) {
        Job job = jobMapper.toEntity(dto);
        job.setActive(true);
        return jobMapper.toResponseDTO(jobRepository.save(job));
    }

    @Transactional(readOnly = true)
    public Page<JobResponseDTO> findAll(
            String keyword,
            String area,
            List<ExperienceLevel> experience,
            String location,
            BigDecimal minSalary,
            Boolean remote,
            Pageable pageable
    ) {
        Specification<Job> spec = Specification.where(JobSpecification.isActive());

        if (keyword != null && !keyword.isBlank()) {
            spec = spec.and(JobSpecification.hasKeyword(keyword));
        }
        if (area != null && !area.isBlank()) {
            spec = spec.and(JobSpecification.hasArea(area));
        }
        if (experience != null && !experience.isEmpty()) {
            spec = spec.and(JobSpecification.hasExperienceLevels(experience));
        }
        if (location != null && !location.isBlank()) {
            spec = spec.and(JobSpecification.hasLocation(location));
        }
        if (minSalary != null) {
            spec = spec.and(JobSpecification.hasSalaryGreaterOrEqual(minSalary));
        }
        if (Boolean.TRUE.equals(remote)) {
            spec = spec.and(JobSpecification.isRemote());
        }

        return jobRepository.findAll(spec, pageable).map(jobMapper::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public JobResponseDTO findById(Long id) {
        Job job = jobRepository.findById(id)
                .filter(Job::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + id));

        return jobMapper.toResponseDTO(job);
    }

    public JobResponseDTO update(Long id, JobRequestDTO dto) {
        Job job = jobRepository.findById(id)
                .filter(Job::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + id));

        jobMapper.updateEntityFromDTO(dto, job);

        return jobMapper.toResponseDTO(jobRepository.save(job));
    }

    public void delete(Long id) {
        Job job = jobRepository.findById(id)
                .filter(Job::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + id));

        job.setActive(false);

        jobRepository.save(job);
    }
}