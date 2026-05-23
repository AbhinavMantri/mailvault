package com.mailvault.ingestion.quota;

public record QuotaReservationRequest(
        String userId,
        long bytes
) {
}
