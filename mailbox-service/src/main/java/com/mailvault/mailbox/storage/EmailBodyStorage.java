package com.mailvault.mailbox.storage;

import com.mailvault.mailbox.config.StorageProperties;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class EmailBodyStorage {

    private final MinioClient minioClient;
    private final StorageProperties properties;

    public EmailBodyStorage(MinioClient minioClient, StorageProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
    }

    public String readText(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }
        try (var stream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(properties.bucket())
                .object(objectKey)
                .build())) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new EmailBodyStorageException("Failed to read email body object", exception);
        }
    }
}
