package com.mailvault.ingestion.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "attachments")
public class Attachment {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String filename;

    @Column(length = 64)
    private String sha256;

    @Column(nullable = false)
    private String objectKey;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private long sizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttachmentStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    protected Attachment() {
    }

    public Attachment(UUID id, String userId, String filename, String objectKey, String contentType,
                      long sizeBytes, AttachmentStatus status, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.filename = filename;
        this.objectKey = objectKey;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getSha256() {
        return sha256;
    }

    public String getUserId() {
        return userId;
    }

    public String getFilename() {
        return filename;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public AttachmentStatus getStatus() {
        return status;
    }

    public void markUploaded() {
        this.status = AttachmentStatus.UPLOADED;
    }
}
