package com.mailvault.attachmentworker.storage;

import com.mailvault.attachmentworker.config.StorageProperties;
import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
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

    public void copyObject(String sourceObjectKey, String targetObjectKey) {
        try {
            minioClient.copyObject(CopyObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(targetObjectKey)
                    .source(CopySource.builder()
                            .bucket(properties.bucket())
                            .object(sourceObjectKey)
                            .build())
                    .build());
        } catch (Exception exception) {
            throw new AttachmentStorageException("Failed to copy attachment object", exception);
        }
    }

    public void deleteObject(String objectKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(objectKey)
                    .build());
        } catch (Exception exception) {
            throw new AttachmentStorageException("Failed to delete attachment object", exception);
        }
    }
}
