package com.alumni.academic_management_api.mapper;

import com.alumni.academic_management_api.dto.user.UserProfileResponseDTO;
import com.alumni.academic_management_api.dto.user.UserSimpleDTO;
import com.alumni.academic_management_api.entity.User;
import com.alumni.academic_management_api.enums.AccountStatus;
import com.alumni.academic_management_api.enums.Role;
import com.alumni.academic_management_api.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private FileStorageService fileStorageService;
    private UserMapper userMapper;

    @BeforeEach
    void setUp() {
        fileStorageService = Mockito.mock(FileStorageService.class);
        userMapper = new UserMapperImpl();
        userMapper.fileStorageService = fileStorageService;
    }

    @Nested
    class ToSimpleDTO {

        @Test
        void givenUserWithProfilePicture_whenToSimpleDTO_thenMapsPresignedUrl() {
            String objectKey = "avatars/aluno.jpg";
            String presignedUrl = "https://s3.us-east-005.backblazeb2.com/alumni-files/avatars/aluno.jpg"
                    + "?X-Amz-Signature=xyz";
            Mockito.when(fileStorageService.generatePresignedUrl(objectKey, 15))
                    .thenReturn(presignedUrl);
            User user = User.builder()
                    .id(1L)
                    .name("Aluno Teste")
                    .email("aluno@teste.com")
                    .profilePictureUrl(objectKey)
                    .accountStatus(AccountStatus.ACTIVE)
                    .role(Role.ALUMNI)
                    .build();

            UserSimpleDTO result = userMapper.toSimpleDTO(user);

            assertThat(result.getProfilePictureUrl()).isEqualTo(presignedUrl);
        }

        @Test
        void givenUserWithoutProfilePicture_whenToSimpleDTO_thenReturnNullWithoutGeneratingUrl() {
            User user = User.builder()
                    .id(1L)
                    .name("Aluno Teste")
                    .email("aluno@teste.com")
                    .accountStatus(AccountStatus.ACTIVE)
                    .role(Role.ALUMNI)
                    .build();

            UserSimpleDTO result = userMapper.toSimpleDTO(user);

            assertThat(result.getProfilePictureUrl()).isNull();
            Mockito.verify(fileStorageService, Mockito.never())
                    .generatePresignedUrl(Mockito.any(), Mockito.anyInt());
        }
    }

    @Nested
    class ToProfileDTO {

        @Test
        void givenUserWithProfilePicture_whenToProfileDTO_thenMapsPresignedUrl() {
            String objectKey = "avatars/aluno.jpg";
            String presignedUrl = "https://s3.us-east-005.backblazeb2.com/alumni-files/avatars/aluno.jpg"
                    + "?X-Amz-Signature=xyz";
            Mockito.when(fileStorageService.generatePresignedUrl(objectKey, 15))
                    .thenReturn(presignedUrl);
            User user = User.builder()
                    .id(1L)
                    .name("Aluno Teste")
                    .email("aluno@teste.com")
                    .profilePictureUrl(objectKey)
                    .accountStatus(AccountStatus.ACTIVE)
                    .role(Role.ALUMNI)
                    .build();

            UserProfileResponseDTO result = userMapper.toProfileDTO(user);

            assertThat(result.getProfilePictureUrl()).isEqualTo(presignedUrl);
        }

        @Test
        void givenUserWithoutProfilePicture_whenToProfileDTO_thenReturnNullWithoutGeneratingUrl() {
            User user = User.builder()
                    .id(1L)
                    .name("Aluno Teste")
                    .email("aluno@teste.com")
                    .accountStatus(AccountStatus.ACTIVE)
                    .role(Role.ALUMNI)
                    .build();

            UserProfileResponseDTO result = userMapper.toProfileDTO(user);

            assertThat(result.getProfilePictureUrl()).isNull();
            Mockito.verify(fileStorageService, Mockito.never())
                    .generatePresignedUrl(Mockito.any(), Mockito.anyInt());
        }
    }
}
