package com.mailvault.ingestion.api.dto;

import java.util.UUID;

public record AttachmentInitiateResponse(
        UUID attachmentId,
        String uploadUrl,
        String objectKey,
        int expiresInSeconds,
        String status
) {
}

