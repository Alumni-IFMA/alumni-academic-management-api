package com.alumni.academic_management_api.service;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    @Mock
    private MinioClient minioClient;

    private FileStorageService fileStorageService;

    private static final String BUCKET = "alumni-files";
    private static final String ENDPOINT = "http://localhost:9000";

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService(minioClient, BUCKET, ENDPOINT);
    }

    @Nested
    class UploadFile {

        @Test
        void givenJpegFile_whenUploadFile_thenReturnPublicUrlWithJpgExtension() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "avatar.jpg", "image/jpeg", new byte[]{1, 2, 3}
            );

            String url = fileStorageService.uploadFile(file, FileStorageService.FOLDER_PROFILE_PICTURES);

            assertThat(url).startsWith(
                    ENDPOINT + "/" + BUCKET + "/" + FileStorageService.FOLDER_PROFILE_PICTURES
            );
            assertThat(url).endsWith(".jpg");
            verify(minioClient).putObject(any(PutObjectArgs.class));
        }

        @Test
        void givenPdfFile_whenUploadFile_thenReturnPublicUrlWithPdfExtension() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "diploma.pdf", "application/pdf", new byte[]{1, 2, 3}
            );

            String url = fileStorageService.uploadFile(file, FileStorageService.FOLDER_DIPLOMAS);

            assertThat(url).startsWith(ENDPOINT + "/" + BUCKET + "/" + FileStorageService.FOLDER_DIPLOMAS);
            assertThat(url).endsWith(".pdf");
        }

        @Test
        void givenFileWithNoExtension_whenUploadFile_thenReturnUrlWithNoExtension() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "noextension", "image/png", new byte[]{1}
            );

            String url = fileStorageService.uploadFile(file, FileStorageService.FOLDER_NEWS_IMAGES);

            assertThat(url).startsWith(ENDPOINT + "/" + BUCKET + "/" + FileStorageService.FOLDER_NEWS_IMAGES);
            assertThat(url).doesNotContain("null");
        }
    }

    @Nested
    class DeleteFile {

        @Test
        void givenPublicUrl_whenDeleteFile_thenCallRemoveObject() throws Exception {
            String url = ENDPOINT + "/" + BUCKET + "/profile-pictures/abc.jpg";

            fileStorageService.deleteFile(url);

            verify(minioClient).removeObject(any(RemoveObjectArgs.class));
        }
    }

    @Nested
    class GeneratePresignedUrl {

        @Test
        void givenFileUrl_whenGeneratePresignedUrl_thenReturnSignedUrl() throws Exception {
            String fileUrl = ENDPOINT + "/" + BUCKET + "/diplomas/abc.pdf";
            String expected = "http://localhost:9000/alumni-files/diplomas/abc.pdf?X-Amz-Signature=xyz";
            when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                    .thenReturn(expected);

            String result = fileStorageService.generatePresignedUrl(fileUrl, 60);

            assertThat(result).isEqualTo(expected);
        }
    }
}
