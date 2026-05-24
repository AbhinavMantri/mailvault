package com.mailvault.mailbox.repository;

import java.time.Instant;
import java.util.UUID;

public record ThreadMessageRow(
        UUID threadId,
        UUID emailId,
        String direction,
        String sender,
        String subject,
        String textObjectKey,
        String htmlObjectKey,
        Instant receivedAt,
        long logicalSizeBytes
) {
}
