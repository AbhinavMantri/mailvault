package com.mailvault.mailbox.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EmailDetailResponse(
        UUID id,
        String userId,
        String sender,
        String subject,
        String textBody,
        String htmlBody,
        String status,
        Instant receivedAt,
        long logicalSizeBytes,
        List<RecipientResponse> recipients,
        List<AttachmentResponse> attachments
) {
}
