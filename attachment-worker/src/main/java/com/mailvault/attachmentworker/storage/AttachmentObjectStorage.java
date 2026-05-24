package com.mailvault.attachmentworker.storage;

import com.mailvault.attachmentworker.config.StorageProperties;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class AttachmentObjectStorage {

    private final MinioClient minioClient;
    private final StorageProperties properties;

    public AttachmentObjectStorage(MinioClient minioClient, StorageProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
    }

    public byte[] readBytes(String objectKey) {
        try (var stream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(properties.bucket())
                .object(objectKey)
                .build())) {
            return stream.readAllBytes();
        } catch (IOException exception) {
            throw new AttachmentStorageException("Failed to read attachment object", exception);
        } catch (Exception exception) {
            throw new AttachmentStorageException("Failed to read attachment object", exception);
        }
    }
}
