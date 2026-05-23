package com.mailvault.mailbox.repository;

import java.util.UUID;

public record AttachmentRow(
        UUID id,
        String filename,
        String contentType,
        long sizeBytes,
        String status
) {
}
