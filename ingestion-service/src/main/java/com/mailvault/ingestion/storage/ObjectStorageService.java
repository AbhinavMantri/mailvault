package com.mailvault.ingestion.storage;

import com.mailvault.ingestion.config.StorageProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.http.Method;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
public class ObjectStorageService {

    private final MinioClient minioClient;
    private final StorageProperties properties;

    public ObjectStorageService(MinioClient minioClient, StorageProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
    }

    public void putText(String objectKey, String content, String contentType) {
        putBytes(objectKey, content.getBytes(StandardCharsets.UTF_8), contentType);
    }

    public void putBytes(String objectKey, byte[] content, String contentType) {
        try {
            ensureBucketExists();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(objectKey)
                    .contentType(contentType)
                    .stream(new ByteArrayInputStream(content), content.length, -1)
                    .build());
        } catch (Exception exception) {
            throw new ObjectStorageException("Failed to write object " + objectKey, exception);
        }
    }

    public String readText(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return "";
        }
        try (var stream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(properties.bucket())
                .object(objectKey)
                .build())) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new ObjectStorageException("Failed to read object " + objectKey, exception);
        }
    }

    public String presignedPutUrl(String objectKey, Duration expiry) {
        try {
            ensureBucketExists();
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(properties.bucket())
                    .object(objectKey)
                    .method(Method.PUT)
                    .expiry((int) expiry.toSeconds())
                    .build());
        } catch (Exception exception) {
            throw new ObjectStorageException("Failed to create upload URL for object " + objectKey, exception);
        }
    }

    private void ensureBucketExists() throws Exception {
        boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(properties.bucket())
                .build());
        if (!bucketExists) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(properties.bucket())
                    .build());
        }
    }
}
