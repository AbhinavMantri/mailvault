package com.mailvault.ingestion.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record EmailImportRequest(
        @NotBlank String userId,
        @Email @NotBlank String from,
        @NotEmpty List<@Email String> to,
        @NotBlank String subject,
        String textBody,
        String htmlBody,
        @Valid List<AttachmentImportRequest> attachments
) {
}

