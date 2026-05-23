package com.mailvault.searchservice.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SearchResultResponse(
        UUID emailId,
        String sender,
        List<String> recipients,
        String subject,
        Instant receivedAt,
        long logicalSizeBytes
) {
}
