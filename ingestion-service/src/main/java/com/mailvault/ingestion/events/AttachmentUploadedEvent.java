package com.mailvault.ingestion.events;

import java.time.Instant;
import java.util.UUID;

public record AttachmentUploadedEvent(
        UUID eventId,
        UUID attachmentId,
        String userId,
        String filename,
        String objectKey,
        String contentType,
        long sizeBytes,
        Instant uploadedAt
) {
}
