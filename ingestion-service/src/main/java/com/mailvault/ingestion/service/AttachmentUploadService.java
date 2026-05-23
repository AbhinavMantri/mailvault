package com.mailvault.ingestion.service;

import com.mailvault.ingestion.api.dto.AttachmentCompleteResponse;
import com.mailvault.ingestion.api.dto.AttachmentInitiateRequest;
import com.mailvault.ingestion.api.dto.AttachmentInitiateResponse;
import com.mailvault.ingestion.domain.Attachment;
import com.mailvault.ingestion.domain.AttachmentStatus;
import com.mailvault.ingestion.repository.AttachmentRepository;
import com.mailvault.ingestion.storage.ObjectStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class AttachmentUploadService {

    private static final Duration UPLOAD_URL_EXPIRY = Duration.ofMinutes(15);

    private final AttachmentRepository attachmentRepository;
    private final ObjectStorageService objectStorageService;

    public AttachmentUploadService(AttachmentRepository attachmentRepository,
                                   ObjectStorageService objectStorageService) {
        this.attachmentRepository = attachmentRepository;
        this.objectStorageService = objectStorageService;
    }

    @Transactional
    public AttachmentInitiateResponse initiateUpload(AttachmentInitiateRequest request) {
        // TODO: enforce attachment size, content type, and per-email attachment count limits.
        UUID attachmentId = UUID.randomUUID();
        String objectKey = "users/%s/pending-attachments/%s/%s"
                .formatted(request.userId(), attachmentId, sanitizeFilename(request.filename()));
        Attachment attachment = new Attachment(
                attachmentId,
                request.userId(),
                request.filename(),
                objectKey,
                request.contentType(),
                request.sizeBytes(),
                AttachmentStatus.PENDING_UPLOAD,
                Instant.now()
        );
        attachmentRepository.save(attachment);

        String uploadUrl = objectStorageService.presignedPutUrl(objectKey, UPLOAD_URL_EXPIRY);
        return new AttachmentInitiateResponse(
                attachmentId,
                uploadUrl,
                objectKey,
                (int) UPLOAD_URL_EXPIRY.toSeconds(),
                AttachmentStatus.PENDING_UPLOAD.name()
        );
    }

    @Transactional
    public AttachmentCompleteResponse completeUpload(UUID attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("Attachment not found"));
        if (attachment.getStatus() != AttachmentStatus.PENDING_UPLOAD) {
            throw new IllegalArgumentException("Attachment is not pending upload");
        }
        // TODO: verify object existence and expected size in object storage before marking uploaded.
        attachment.markUploaded();
        attachmentRepository.save(attachment);
        return new AttachmentCompleteResponse(attachmentId, attachment.getStatus().name());
    }

    private String sanitizeFilename(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
