package com.mailvault.quota.api;

public record QuotaReservationResponse(
        String userId,
        long reservedBytes,
        long usedBytes,
        long quotaBytes,
        String status
) {
}
