package com.mailvault.mailbox.api;

import java.util.UUID;

public record ThreadActionResponse(
        UUID threadId,
        String status,
        int unreadCount,
        String folder
) {
}
