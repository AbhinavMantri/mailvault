package com.mailvault.mailbox.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ThreadMessageResponse(
        UUID emailId,
        String direction,
        String sender,
        String subject,
        String textBody,
        String htmlBody,
        Instant receivedAt,
        long logicalSizeBytes,
        List<RecipientResponse> recipients,
        List<AttachmentResponse> attachments
) {
}
