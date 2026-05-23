package com.mailvault.ingestion.api.dto;

import java.util.UUID;

public record AttachmentCompleteResponse(
        UUID attachmentId,
        String status
) {
}

