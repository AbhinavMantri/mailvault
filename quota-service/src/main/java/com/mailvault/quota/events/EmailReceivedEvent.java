package com.mailvault.quota.events;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EmailReceivedEvent(
        UUID eventId,
        UUID emailId,
        String userId,
        String sender,
        List<String> recipients,
        String subject,
        long logicalSizeBytes,
        Instant receivedAt
) {
}
