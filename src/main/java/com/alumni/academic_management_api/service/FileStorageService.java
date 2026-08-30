package com.alumni.academic_management_api.service;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class FileStorageService {

    public static final String FOLDER_PROFILE_PICTURES = "profile-pictures/";
    public static final String FOLDER_NEWS_IMAGES = "news-images/";
    public static final String FOLDER_DIPLOMAS = "diplomas/";

    private final MinioClient minioClient;
    private final String bucketName;
    private final String endpoint;

    public FileStorageService(MinioClient minioClient,
                               @Value("${minio.bucket-name}") String bucketName,
                               @Value("${minio.endpoint}") String endpoint) {
        this.minioClient = minioClient;
        this.bucketName = bucketName;
        this.endpoint = endpoint;
    }

    public String uploadFile(MultipartFile file, String folder) {
        String objectName = folder + UUID.randomUUID() + getExtension(file.getOriginalFilename());
        try (InputStream stream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(stream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
        }
        return endpoint + "/" + bucketName + "/" + objectName;
    }

    public void deleteFile(String fileUrl) {
        String objectName = extractObjectName(fileUrl);
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete file: " + e.getMessage(), e);
        }
    }

    public String generatePresignedUrl(String fileUrl, int expiryMinutes) {
        String objectName = extractObjectName(fileUrl);
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(expiryMinutes, TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate presigned URL: " + e.getMessage(), e);
        }
    }

    public String extractObjectName(String fileUrl) {
        String prefix = endpoint + "/" + bucketName + "/";
        return fileUrl.startsWith(prefix) ? fileUrl.substring(prefix.length()) : fileUrl;
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }
}
