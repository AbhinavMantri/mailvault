package com.mailvault.attachmentworker.service;

import com.mailvault.attachmentworker.repository.AttachmentBlobRow;
import com.mailvault.attachmentworker.repository.AttachmentRepository;
import com.mailvault.attachmentworker.repository.AttachmentRow;
import com.mailvault.attachmentworker.storage.AttachmentObjectStorage;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class AttachmentProcessingService {

    private final AttachmentRepository attachmentRepository;
    private final AttachmentObjectStorage objectStorage;

    public AttachmentProcessingService(
            AttachmentRepository attachmentRepository,
            AttachmentObjectStorage objectStorage
    ) {
        this.attachmentRepository = attachmentRepository;
        this.objectStorage = objectStorage;
    }

    public void process(AttachmentRow attachment) {
        if (!attachmentRepository.markProcessing(attachment.id())) {
            return;
        }

        try {
            byte[] bytes = objectStorage.readBytes(attachment.objectKey());
            String sha256 = sha256(bytes);
            AttachmentBlobRow blob = resolveBlob(attachment, sha256);
            attachmentRepository.incrementBlobRefCount(blob.id());
            attachmentRepository.markReady(attachment.id(), sha256, blob.id(), blob.objectKey());
        } catch (RuntimeException exception) {
            attachmentRepository.markFailed(attachment.id());
            throw exception;
        }
        deletePendingObject(attachment.objectKey());
    }

    public void process(UUID attachmentId) {
        attachmentRepository.findById(attachmentId).ifPresent(this::process);
    }

    private AttachmentBlobRow resolveBlob(AttachmentRow attachment, String sha256) {
        return attachmentRepository.findBlobBySha256(sha256)
                .orElseGet(() -> createBlob(attachment, sha256));
    }

    private AttachmentBlobRow createBlob(AttachmentRow attachment, String sha256) {
        String canonicalObjectKey = canonicalObjectKey(sha256);
        AttachmentBlobRow blob = new AttachmentBlobRow(UUID.randomUUID(), sha256, canonicalObjectKey, attachment.sizeBytes(), 0);
        objectStorage.copyObject(attachment.objectKey(), canonicalObjectKey);
        boolean inserted = attachmentRepository.insertBlob(blob, Instant.now());
        if (inserted) {
            return blob;
        }
        return attachmentRepository.findBlobBySha256(sha256)
                .orElseThrow(() -> new IllegalStateException("Attachment blob was not created"));
    }

    private String canonicalObjectKey(String sha256) {
        return "attachments/blobs/sha256/%s/%s".formatted(sha256.substring(0, 2), sha256);
    }

    private void deletePendingObject(String objectKey) {
        try {
            objectStorage.deleteObject(objectKey);
        } catch (RuntimeException exception) {
            // The attachment already points at the canonical blob; retry cleanup can remove this orphan later.
        }
    }

    private String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest is not available", exception);
        }
    }
}
