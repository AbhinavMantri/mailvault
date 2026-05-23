package com.mailvault.ingestion.api.dto;

import jakarta.validation.constraints.NotBlank;

public record AttachmentImportRequest(
        @NotBlank String filename,
        @NotBlank String contentType,
        @NotBlank String base64Content
) {
}

