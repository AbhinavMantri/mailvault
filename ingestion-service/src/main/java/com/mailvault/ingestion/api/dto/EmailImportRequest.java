package com.mailvault.ingestion.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record EmailImportRequest(
        @NotBlank String userId,
        @Email @NotBlank String from,
        @NotEmpty List<@Email String> to,
        List<@Email String> cc,
        List<@Email String> bcc,
        @NotBlank String subject,
        String textBody,
        String htmlBody,
        List<UUID> attachmentIds
) {
}
