package com.mailvault.mailbox.repository;

import java.time.Instant;
import java.util.UUID;

public record InboxRow(
        UUID id,
        String sender,
        String subject,
        String status,
        Instant receivedAt,
        long logicalSizeBytes,
        int attachmentCount
) {
}
