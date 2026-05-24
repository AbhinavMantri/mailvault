package com.mailvault.attachmentworker.config;

import io.minio.MinioClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class AttachmentWorkerConfiguration {

    @Bean
    MinioClient minioClient(StorageProperties properties) {
        return MinioClient.builder()
                .endpoint(properties.endpoint())
                .credentials(properties.accessKey(), properties.secretKey())
                .build();
    }
}
