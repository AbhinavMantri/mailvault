package com.mailvault.quota.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record QuotaReservationRequest(
        @NotBlank String userId,
        @Min(1) long bytes
) {
}
