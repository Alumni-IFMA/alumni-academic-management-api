package com.alumni.academic_management_api.service;

import com.alumni.academic_management_api.dto.degree.DegreeRequestDTO;
import com.alumni.academic_management_api.dto.degree.DegreeResponseDTO;
import com.alumni.academic_management_api.entity.Degree;
import com.alumni.academic_management_api.entity.User;
import com.alumni.academic_management_api.exception.BusinessException;
import com.alumni.academic_management_api.exception.ResourceNotFoundException;
import com.alumni.academic_management_api.mapper.DegreeMapper;
import com.alumni.academic_management_api.repository.DegreeRepository;
import com.alumni.academic_management_api.repository.UserRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class DegreeServiceTest {

    @Mock
    private DegreeRepository degreeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DegreeMapper degreeMapper;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private DegreeService degreeService;

    @Nested
    class Upload {

        @Test
        void givenValidPdfAndExistingUser_whenUpload_thenSaveDegreeAndReturnResponse() {
            Long userId = 1L;
            User user = User.builder().id(userId).build();
            MockMultipartFile file = new MockMultipartFile(
                    "file", "diploma.pdf", "application/pdf", new byte[]{1, 2, 3}
            );
            DegreeRequestDTO request = DegreeRequestDTO.builder()
                    .userId(userId)
                    .title("Bacharelado em Ciência da Computação")
                    .file(file)
                    .build();
            String fileUrl = "http://localhost:9000/alumni-files/diplomas/uuid.pdf";
            DegreeResponseDTO expectedResponse = DegreeResponseDTO.builder()
                    .id(10L)
                    .title(request.getTitle())
                    .userId(userId)
                    .fileUrl(fileUrl)
                    .build();

            Mockito.when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            Mockito.when(fileStorageService.uploadFile(file, FileStorageService.FOLDER_DIPLOMAS))
                    .thenReturn(fileUrl);
            Mockito.when(degreeRepository.save(any(Degree.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            Mockito.when(degreeMapper.toResponseDTO(any(Degree.class))).thenReturn(expectedResponse);

            DegreeResponseDTO result = degreeService.upload(request);

            assertThat(result).isEqualTo(expectedResponse);
            Mockito.verify(fileStorageService).uploadFile(file, FileStorageService.FOLDER_DIPLOMAS);
        }

        @Test
        void givenNonPdfFile_whenUpload_thenThrowBusinessException() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "diploma.jpg", "image/jpeg", new byte[]{1}
            );
            DegreeRequestDTO request = DegreeRequestDTO.builder()
                    .userId(1L)
                    .title("Bacharelado em Ciência da Computação")
                    .file(file)
                    .build();

            assertThatThrownBy(() -> degreeService.upload(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Only PDF files are allowed");

            Mockito.verifyNoInteractions(userRepository, fileStorageService, degreeRepository);
        }

        @Test
        void givenNonExistentUser_whenUpload_thenThrowResourceNotFoundException() {
            Long userId = 999L;
            MockMultipartFile file = new MockMultipartFile(
                    "file", "diploma.pdf", "application/pdf", new byte[]{1}
            );
            DegreeRequestDTO request = DegreeRequestDTO.builder()
                    .userId(userId)
                    .title("Bacharelado em Ciência da Computação")
                    .file(file)
                    .build();
            Mockito.when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> degreeService.upload(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("999");

            Mockito.verifyNoInteractions(fileStorageService, degreeRepository);
        }
    }

    @Nested
    class FindByAuthenticatedUser {

        @Test
        void givenUserWithDegrees_whenFindByAuthenticatedUser_thenReturnMappedList() {
            String email = "user@email.com";
            User user = User.builder().id(1L).email(email).build();
            Degree degree = Degree.builder().id(5L).title("Bacharelado").user(user).build();
            DegreeResponseDTO responseDTO = DegreeResponseDTO.builder()
                    .id(5L).title("Bacharelado").userId(1L).build();

            Mockito.when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
            Mockito.when(degreeRepository.findByUserId(1L)).thenReturn(List.of(degree));
            Mockito.when(degreeMapper.toResponseDTO(degree)).thenReturn(responseDTO);

            List<DegreeResponseDTO> result = degreeService.findByAuthenticatedUser(email);

            assertThat(result).containsExactly(responseDTO);
        }

        @Test
        void givenUserWithNoDegrees_whenFindByAuthenticatedUser_thenReturnEmptyList() {
            String email = "user@email.com";
            User user = User.builder().id(1L).email(email).build();

            Mockito.when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
            Mockito.when(degreeRepository.findByUserId(1L)).thenReturn(List.of());

            List<DegreeResponseDTO> result = degreeService.findByAuthenticatedUser(email);

            assertThat(result).isEmpty();
        }

        @Test
        void givenNonExistentEmail_whenFindByAuthenticatedUser_thenThrowResourceNotFoundException() {
            String email = "missing@email.com";
            Mockito.when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> degreeService.findByAuthenticatedUser(email))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    class GenerateDownloadUrl {

        @Test
        void givenOwnedDegree_whenGenerateDownloadUrl_thenReturnPresignedUrl() {
            String email = "user@email.com";
            User owner = User.builder().id(1L).email(email).build();
            Degree degree = Degree.builder()
                    .id(5L)
                    .fileUrl("http://localhost:9000/alumni-files/diplomas/uuid.pdf")
                    .user(owner)
                    .build();
            String presignedUrl = "http://localhost:9000/alumni-files/diplomas/uuid.pdf?X-Amz-Signature=xyz";

            Mockito.when(degreeRepository.findById(5L)).thenReturn(Optional.of(degree));
            Mockito.when(fileStorageService.generatePresignedUrl(degree.getFileUrl(), 15))
                    .thenReturn(presignedUrl);

            String result = degreeService.generateDownloadUrl(5L, email);

            assertThat(result).isEqualTo(presignedUrl);
        }

        @Test
        void givenNonExistentDegree_whenGenerateDownloadUrl_thenThrowResourceNotFoundException() {
            Mockito.when(degreeRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> degreeService.generateDownloadUrl(999L, "user@email.com"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("999");
        }

        @Test
        void givenDegreeOwnedByAnotherUser_whenGenerateDownloadUrl_thenThrowBusinessException() {
            User owner = User.builder().id(1L).email("owner@email.com").build();
            Degree degree = Degree.builder().id(5L).fileUrl("http://localhost:9000/x.pdf").user(owner).build();

            Mockito.when(degreeRepository.findById(5L)).thenReturn(Optional.of(degree));

            assertThatThrownBy(() -> degreeService.generateDownloadUrl(5L, "someone-else@email.com"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Degree does not belong to the authenticated user");

            Mockito.verifyNoInteractions(fileStorageService);
        }
    }
}
