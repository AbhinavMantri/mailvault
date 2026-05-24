package com.mailvault.attachmentworker.repository;

import java.util.UUID;

public record AttachmentBlobRow(
        UUID id,
        String sha256,
        String objectKey,
        long sizeBytes,
        long refCount
) {
}
