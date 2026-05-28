package com.mailvault.ingestion.api.dto;

import jakarta.validation.constraints.NotBlank;

public record SendDraftRequest(
        @NotBlank String userId
) {
}
