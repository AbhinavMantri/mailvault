package com.mailvault.mailbox.api;

import com.mailvault.mailbox.domain.EmailStatus;

import java.time.Instant;
import java.util.UUID;

public record InboxItemResponse(
        UUID id,
        String sender,
        String subject,
        EmailStatus status,
        Instant receivedAt,
        long logicalSizeBytes,
        int attachmentCount
) {
}
