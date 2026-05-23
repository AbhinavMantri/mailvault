package com.mailvault.mailbox.api;

import java.time.Instant;

public record StorageUsageResponse(
        String userId,
        long usedBytes,
        long quotaBytes,
        double usedPercent,
        Instant updatedAt
) {
}
