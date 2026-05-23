package com.mailvault.quota.repository;

import java.time.Instant;

public record StorageUsageRow(
        String userId,
        long usedBytes,
        long quotaBytes,
        Instant updatedAt
) {
}
