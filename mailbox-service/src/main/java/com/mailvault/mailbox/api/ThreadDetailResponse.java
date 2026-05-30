package com.mailvault.mailbox.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ThreadDetailResponse(
        UUID id,
        String userId,
        String subject,
        String folder,
        Instant lastMessageAt,
        int messageCount,
        int unreadCount,
        List<String> labels,
        List<ThreadMessageResponse> messages
) {
}
