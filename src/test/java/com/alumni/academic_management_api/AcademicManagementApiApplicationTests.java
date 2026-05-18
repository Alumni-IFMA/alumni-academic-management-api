package com.alumni.academic_management_api;

import io.minio.MinioClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class AcademicManagementApiApplicationTests {

    @MockBean
    private MinioClient minioClient;

    @Test
    void contextLoads() {
    }

}