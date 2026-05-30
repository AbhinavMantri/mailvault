package com.mailvault.mailbox.api;

import java.util.List;
import java.util.UUID;

public record ThreadLabelActionResponse(
        UUID threadId,
        String status,
        String label,
        String source,
        List<ThreadLabelResponse> labels
) {
}
