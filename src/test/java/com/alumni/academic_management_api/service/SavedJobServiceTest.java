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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class SavedJobServiceTest {

    @Mock
    private SavedJobRepository savedJobRepository;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JobMapper jobMapper;

    @InjectMocks
    private SavedJobService savedJobService;

    @Nested
    class SaveJob {

        @Test
        void givenActiveJobNotYetSaved_whenSaveJob_thenCreateSavedJob() {
            Long jobId = 5L;
            User user = User.builder().id(1L).email("user@test.com").build();
            Job job = Job.builder().id(jobId).active(true).build();

            Mockito.when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
            Mockito.when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
            Mockito.when(savedJobRepository.existsByUserIdAndJobId(user.getId(), jobId)).thenReturn(false);

            savedJobService.saveJob(user.getEmail(), jobId);

            ArgumentCaptor<SavedJob> captor = ArgumentCaptor.forClass(SavedJob.class);
            Mockito.verify(savedJobRepository).save(captor.capture());
            SavedJob savedJob = captor.getValue();
            assertThat(savedJob.getUser()).isEqualTo(user);
            assertThat(savedJob.getJob()).isEqualTo(job);
        }

        @Test
        void givenAlreadySavedJob_whenSaveJob_thenThrowBusinessException() {
            Long jobId = 5L;
            User user = User.builder().id(1L).email("user@test.com").build();
            Job job = Job.builder().id(jobId).active(true).build();

            Mockito.when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
            Mockito.when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
            Mockito.when(savedJobRepository.existsByUserIdAndJobId(user.getId(), jobId)).thenReturn(true);

            assertThatThrownBy(() -> savedJobService.saveJob(user.getEmail(), jobId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("already saved");

            Mockito.verify(savedJobRepository, Mockito.never()).save(Mockito.any());
        }

        @Test
        void givenMissingJob_whenSaveJob_thenThrowResourceNotFoundException() {
            Long jobId = 99L;
            User user = User.builder().id(1L).email("user@test.com").build();

            Mockito.when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
            Mockito.when(jobRepository.findById(jobId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> savedJobService.saveJob(user.getEmail(), jobId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");

            Mockito.verify(savedJobRepository, Mockito.never()).save(Mockito.any());
        }

        @Test
        void givenInactiveJob_whenSaveJob_thenThrowResourceNotFoundException() {
            Long jobId = 5L;
            User user = User.builder().id(1L).email("user@test.com").build();
            Job job = Job.builder().id(jobId).active(false).build();

            Mockito.when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
            Mockito.when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

            assertThatThrownBy(() -> savedJobService.saveJob(user.getEmail(), jobId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("5");

            Mockito.verify(savedJobRepository, Mockito.never()).save(Mockito.any());
        }

        @Test
        void givenMissingAuthenticatedUser_whenSaveJob_thenThrowResourceNotFoundException() {
            Mockito.when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> savedJobService.saveJob("ghost@test.com", 5L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found");

            Mockito.verify(savedJobRepository, Mockito.never()).save(Mockito.any());
        }
    }

    @Nested
    class UnsaveJob {

        @Test
        void givenSavedJob_whenUnsaveJob_thenDeleteSavedJob() {
            Long jobId = 5L;
            User user = User.builder().id(1L).email("user@test.com").build();
            SavedJob savedJob = SavedJob.builder().id(10L).user(user).build();

            Mockito.when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
            Mockito.when(savedJobRepository.findByUserIdAndJobId(user.getId(), jobId))
                    .thenReturn(Optional.of(savedJob));

            savedJobService.unsaveJob(user.getEmail(), jobId);

            Mockito.verify(savedJobRepository).delete(savedJob);
        }

        @Test
        void givenJobNotSaved_whenUnsaveJob_thenThrowResourceNotFoundException() {
            Long jobId = 5L;
            User user = User.builder().id(1L).email("user@test.com").build();

            Mockito.when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
            Mockito.when(savedJobRepository.findByUserIdAndJobId(user.getId(), jobId))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> savedJobService.unsaveJob(user.getEmail(), jobId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("5");

            Mockito.verify(savedJobRepository, Mockito.never()).delete(Mockito.any());
        }

        @Test
        void givenMissingAuthenticatedUser_whenUnsaveJob_thenThrowResourceNotFoundException() {
            Mockito.when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> savedJobService.unsaveJob("ghost@test.com", 5L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found");

            Mockito.verify(savedJobRepository, Mockito.never()).delete(Mockito.any());
        }
    }

    @Nested
    class FindSavedJobs {

        @Test
        void givenSavedJobs_whenFindSavedJobs_thenReturnJobList() {
            User user = User.builder().id(1L).email("user@test.com").build();
            Job job = Job.builder().id(5L).title("Java Developer").build();
            SavedJob savedJob = SavedJob.builder().id(10L).user(user).job(job).build();
            JobResponseDTO jobResponseDTO = JobResponseDTO.builder().id(5L).title("Java Developer").build();

            Mockito.when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
            Mockito.when(savedJobRepository.findByUserIdOrderByCreatedAtDesc(user.getId()))
                    .thenReturn(List.of(savedJob));
            Mockito.when(jobMapper.toResponseDTO(job)).thenReturn(jobResponseDTO);

            List<JobResponseDTO> result = savedJobService.findSavedJobs(user.getEmail());

            assertThat(result).containsExactly(jobResponseDTO);
        }

        @Test
        void givenMissingAuthenticatedUser_whenFindSavedJobs_thenThrowResourceNotFoundException() {
            Mockito.when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> savedJobService.findSavedJobs("ghost@test.com"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User not found");

            Mockito.verify(savedJobRepository, Mockito.never()).findByUserIdOrderByCreatedAtDesc(Mockito.anyLong());
        }
    }
}
