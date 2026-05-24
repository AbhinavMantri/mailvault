package com.mailvault.mailbox.repository;

import java.time.Instant;
import java.util.UUID;

public record EmailHeaderRow(
        UUID id,
        String userId,
        String sender,
        String subject,
        String textObjectKey,
        String htmlObjectKey,
        String status,
        Instant receivedAt,
        long logicalSizeBytes
) {
}
