package com.alumni.academic_management_api.service;

import com.alumni.academic_management_api.dto.job.JobResponseDTO;
import com.alumni.academic_management_api.entity.Job;
import com.alumni.academic_management_api.entity.SavedJob;
import com.alumni.academic_management_api.entity.User;
import com.alumni.academic_management_api.exception.BusinessException;
import com.alumni.academic_management_api.exception.ResourceNotFoundException;
import com.alumni.academic_management_api.mapper.JobMapper;
import com.alumni.academic_management_api.repository.JobRepository;
import com.alumni.academic_management_api.repository.SavedJobRepository;
import com.alumni.academic_management_api.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
@Service
public class SavedJobService {

    private final SavedJobRepository savedJobRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final JobMapper jobMapper;

    public SavedJobService(
            SavedJobRepository savedJobRepository,
            JobRepository jobRepository,
            UserRepository userRepository,
            JobMapper jobMapper
    ) {
        this.savedJobRepository = savedJobRepository;
        this.jobRepository = jobRepository;
        this.userRepository = userRepository;
        this.jobMapper = jobMapper;
    }

    public void saveJob(String userEmail, Long jobId) {
        User user = findAuthenticatedUser(userEmail);
        Job job = jobRepository.findById(jobId)
                .filter(Job::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with id: " + jobId));

        if (savedJobRepository.existsByUserIdAndJobId(user.getId(), jobId)) {
            throw new BusinessException("Job already saved");
        }

        SavedJob savedJob = SavedJob.builder()
                .user(user)
                .job(job)
                .build();

        savedJobRepository.save(savedJob);
    }

    public void unsaveJob(String userEmail, Long jobId) {
        User user = findAuthenticatedUser(userEmail);
        SavedJob savedJob = savedJobRepository.findByUserIdAndJobId(user.getId(), jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Saved job not found for job id: " + jobId));

        savedJobRepository.delete(savedJob);
    }

    @Transactional(readOnly = true)
    public List<JobResponseDTO> findSavedJobs(String userEmail) {
        User user = findAuthenticatedUser(userEmail);
        return savedJobRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(savedJob -> jobMapper.toResponseDTO(savedJob.getJob()))
                .toList();
    }

    private User findAuthenticatedUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
