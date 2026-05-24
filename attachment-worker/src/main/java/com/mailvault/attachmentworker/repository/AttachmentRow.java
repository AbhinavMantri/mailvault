package com.mailvault.attachmentworker.repository;

import java.util.UUID;

public record AttachmentRow(
        UUID id,
        String userId,
        String filename,
        String objectKey,
        long sizeBytes
) {
}
