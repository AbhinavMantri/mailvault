package com.mailvault.searchservice.opensearch;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SearchDocument(
        UUID emailId,
        String userId,
        String sender,
        List<String> recipients,
        String subject,
        long logicalSizeBytes,
        Instant receivedAt
) {
}
