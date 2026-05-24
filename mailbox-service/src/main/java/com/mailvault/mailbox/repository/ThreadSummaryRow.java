package com.mailvault.mailbox.repository;

import java.time.Instant;
import java.util.UUID;

public record ThreadSummaryRow(
        UUID id,
        String subject,
        String folder,
        String lastSender,
        Instant lastMessageAt,
        int messageCount,
        int unreadCount,
        int attachmentCount
) {
}
