package com.mailvault.ingestion.api.dto;

import java.util.UUID;

public record EmailImportResponse(
        UUID emailId,
        String status,
        long logicalSizeBytes
) {
}

