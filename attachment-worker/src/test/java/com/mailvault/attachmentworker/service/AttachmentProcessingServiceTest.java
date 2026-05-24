package com.mailvault.attachmentworker.service;

import com.mailvault.attachmentworker.repository.AttachmentRepository;
import com.mailvault.attachmentworker.repository.AttachmentBlobRow;
import com.mailvault.attachmentworker.repository.AttachmentRow;
import com.mailvault.attachmentworker.storage.AttachmentStorageException;
import com.mailvault.attachmentworker.storage.AttachmentObjectStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttachmentProcessingServiceTest {

    @Mock
    private AttachmentRepository attachmentRepository;

    @Mock
    private AttachmentObjectStorage objectStorage;

    @InjectMocks
    private AttachmentProcessingService attachmentProcessingService;

    @Test
    void processComputesSha256AndMarksAttachmentReady() {
        UUID attachmentId = UUID.randomUUID();
        String sha256 = "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824";
        String canonicalObjectKey = "attachments/blobs/sha256/2c/%s".formatted(sha256);
        AttachmentRow attachment = new AttachmentRow(
                attachmentId,
                "user-123",
                "invoice.txt",
                "users/user-123/pending-attachments/%s/invoice.txt".formatted(attachmentId),
                5
        );
        when(attachmentRepository.markProcessing(attachmentId)).thenReturn(true);
        when(objectStorage.readBytes(attachment.objectKey())).thenReturn("hello".getBytes(StandardCharsets.UTF_8));
        when(attachmentRepository.findBlobBySha256(sha256)).thenReturn(Optional.empty());
        when(attachmentRepository.insertBlob(org.mockito.ArgumentMatchers.any(AttachmentBlobRow.class), org.mockito.ArgumentMatchers.any()))
                .thenReturn(true);

        attachmentProcessingService.process(attachment);

        verify(objectStorage).copyObject(attachment.objectKey(), canonicalObjectKey);
        verify(attachmentRepository).incrementBlobRefCount(org.mockito.ArgumentMatchers.any(UUID.class));
        verify(attachmentRepository).markReady(
                org.mockito.ArgumentMatchers.eq(attachmentId),
                org.mockito.ArgumentMatchers.eq(sha256),
                org.mockito.ArgumentMatchers.any(UUID.class),
                org.mockito.ArgumentMatchers.eq(canonicalObjectKey)
        );
        verify(objectStorage).deleteObject(attachment.objectKey());
        verify(attachmentRepository, never()).markFailed(attachmentId);
    }

    @Test
    void processReusesExistingBlobForDuplicateContent() {
        UUID attachmentId = UUID.randomUUID();
        UUID blobId = UUID.randomUUID();
        String sha256 = "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824";
        String canonicalObjectKey = "attachments/blobs/sha256/2c/%s".formatted(sha256);
        AttachmentRow attachment = new AttachmentRow(attachmentId, "user-123", "invoice.txt", "pending-key", 5);
        AttachmentBlobRow blob = new AttachmentBlobRow(blobId, sha256, canonicalObjectKey, 5, 1);
        when(attachmentRepository.markProcessing(attachmentId)).thenReturn(true);
        when(objectStorage.readBytes(attachment.objectKey())).thenReturn("hello".getBytes(StandardCharsets.UTF_8));
        when(attachmentRepository.findBlobBySha256(sha256)).thenReturn(Optional.of(blob));

        attachmentProcessingService.process(attachment);

        verify(objectStorage, never()).copyObject(attachment.objectKey(), canonicalObjectKey);
        verify(attachmentRepository).incrementBlobRefCount(blobId);
        verify(attachmentRepository).markReady(attachmentId, sha256, blobId, canonicalObjectKey);
        verify(objectStorage).deleteObject(attachment.objectKey());
    }

    @Test
    void processKeepsAttachmentReadyIfPendingObjectCleanupFails() {
        UUID attachmentId = UUID.randomUUID();
        UUID blobId = UUID.randomUUID();
        String sha256 = "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824";
        String canonicalObjectKey = "attachments/blobs/sha256/2c/%s".formatted(sha256);
        AttachmentRow attachment = new AttachmentRow(attachmentId, "user-123", "invoice.txt", "pending-key", 5);
        AttachmentBlobRow blob = new AttachmentBlobRow(blobId, sha256, canonicalObjectKey, 5, 1);
        when(attachmentRepository.markProcessing(attachmentId)).thenReturn(true);
        when(objectStorage.readBytes(attachment.objectKey())).thenReturn("hello".getBytes(StandardCharsets.UTF_8));
        when(attachmentRepository.findBlobBySha256(sha256)).thenReturn(Optional.of(blob));
        doThrow(new AttachmentStorageException("cleanup failed", new RuntimeException("delete failed")))
                .when(objectStorage)
                .deleteObject(attachment.objectKey());

        attachmentProcessingService.process(attachment);

        verify(attachmentRepository).markReady(attachmentId, sha256, blobId, canonicalObjectKey);
        verify(attachmentRepository, never()).markFailed(attachmentId);
    }

    @Test
    void processSkipsAttachmentIfClaimFails() {
        UUID attachmentId = UUID.randomUUID();
        AttachmentRow attachment = new AttachmentRow(attachmentId, "user-123", "invoice.txt", "object-key", 5);
        when(attachmentRepository.markProcessing(attachmentId)).thenReturn(false);

        attachmentProcessingService.process(attachment);

        verify(objectStorage, never()).readBytes(attachment.objectKey());
        verify(attachmentRepository, never()).markReady(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }
}
