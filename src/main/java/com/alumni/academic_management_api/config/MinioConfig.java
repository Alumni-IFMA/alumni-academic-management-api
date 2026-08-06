package com.alumni.academic_management_api.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class MinioConfig {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Bean
    public MinioClient minioClient() {
        try {
            MinioClient client = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();
            initializeBucket(client);
            return client;
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize MinIO client", e);
        }
    }

    private void initializeBucket(MinioClient client) throws Exception {
        boolean exists = client.bucketExists(
                BucketExistsArgs.builder().bucket(bucketName).build()
        );
        if (!exists) {
            client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
        try {
            client.setBucketPolicy(SetBucketPolicyArgs.builder()
                    .bucket(bucketName)
                    .config(buildPublicReadPolicy())
                    .build());
        } catch (Exception e) {
            log.warn("Failed to apply bucket policy for '{}' (provider may not support PutBucketPolicy): {}",
                    bucketName, e.getMessage());
        }
    }

    private String buildPublicReadPolicy() {
        return String.format("""
                {
                  "Version": "2012-10-17",
                  "Statement": [{
                    "Effect": "Allow",
                    "Principal": {"AWS": ["*"]},
                    "Action": ["s3:GetObject"],
                    "Resource": [
                      "arn:aws:s3:::%s/profile-pictures/*",
                      "arn:aws:s3:::%s/news-images/*"
                    ]
                  }]
                }""", bucketName, bucketName);
    }
}