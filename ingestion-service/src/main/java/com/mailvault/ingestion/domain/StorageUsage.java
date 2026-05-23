package com.mailvault.ingestion.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "storage_usage")
public class StorageUsage {

    @Id
    private String userId;

    @Column(nullable = false)
    private long usedBytes;

    @Column(nullable = false)
    private long quotaBytes;

    @Column(nullable = false)
    private Instant updatedAt;

    protected StorageUsage() {
    }

    public StorageUsage(String userId, long usedBytes, long quotaBytes, Instant updatedAt) {
        this.userId = userId;
        this.usedBytes = usedBytes;
        this.quotaBytes = quotaBytes;
        this.updatedAt = updatedAt;
    }

    public void addUsage(long bytes, Instant updatedAt) {
        this.usedBytes += bytes;
        this.updatedAt = updatedAt;
    }

    public boolean canAccept(long bytes) {
        return usedBytes + bytes <= quotaBytes;
    }
}

