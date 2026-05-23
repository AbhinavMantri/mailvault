package com.mailvault.mailbox.api;

import com.mailvault.mailbox.domain.AttachmentStatus;

import java.util.UUID;

public record AttachmentResponse(
        UUID id,
        String filename,
        String contentType,
        long sizeBytes,
        AttachmentStatus status
) {
}
