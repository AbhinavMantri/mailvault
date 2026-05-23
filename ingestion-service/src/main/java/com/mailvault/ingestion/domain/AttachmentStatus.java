package com.mailvault.ingestion.domain;

public enum AttachmentStatus {
    PENDING_UPLOAD,
    UPLOADED,
    PROCESSING,
    READY,
    FAILED,
    QUARANTINED
}
