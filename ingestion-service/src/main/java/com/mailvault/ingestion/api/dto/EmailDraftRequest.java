package com.mailvault.ingestion.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.UUID;

public record EmailDraftRequest(
        @NotBlank String userId,
        @Email @NotBlank String from,
        List<@Email String> to,
        List<@Email String> cc,
        List<@Email String> bcc,
        String subject,
        String textBody,
        String htmlBody,
        List<UUID> attachmentIds
) {
}
