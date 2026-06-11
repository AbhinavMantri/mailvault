package com.mailvault.searchindexer.search;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EmailSearchDocument(
        String documentId,
        UUID emailId,
        String userId,
        String sender,
        List<String> recipients,
        String subject,
        long logicalSizeBytes,
        Instant receivedAt
) {
    public static EmailSearchDocument forUserEmail(
            UUID emailId,
            String userId,
            String sender,
            List<String> recipients,
            String subject,
            long logicalSizeBytes,
            Instant receivedAt
    ) {
        return new EmailSearchDocument(
                "%s:%s".formatted(userId, emailId),
                emailId,
                userId,
                sender,
                recipients,
                subject,
                logicalSizeBytes,
                receivedAt
        );
    }
}
