package com.mailvault.mailbox.domain;

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

    public String getUserId() {
        return userId;
    }

    public long getUsedBytes() {
        return usedBytes;
    }

    public long getQuotaBytes() {
        return quotaBytes;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
