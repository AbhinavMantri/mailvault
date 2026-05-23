package com.mailvault.ingestion.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record AttachmentInitiateRequest(
        @NotBlank String userId,
        @NotBlank String filename,
        @NotBlank String contentType,
        @Positive long sizeBytes
) {
}

