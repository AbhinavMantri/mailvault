package com.mailvault.mailbox.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ThreadSummaryResponse(
        UUID id,
        String subject,
        String folder,
        String lastSender,
        Instant lastMessageAt,
        int messageCount,
        int unreadCount,
        int attachmentCount,
        List<ThreadLabelResponse> labels
) {
}
