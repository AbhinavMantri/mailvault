package com.mailvault.mailbox.api;

import com.mailvault.mailbox.domain.EmailStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EmailDetailResponse(
        UUID id,
        String userId,
        String sender,
        String subject,
        EmailStatus status,
        Instant receivedAt,
        long logicalSizeBytes,
        List<RecipientResponse> recipients,
        List<AttachmentResponse> attachments
) {
}
