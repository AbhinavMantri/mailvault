package com.mailvault.attachmentworker.service;

import com.mailvault.attachmentworker.repository.AttachmentRepository;
import com.mailvault.attachmentworker.repository.AttachmentRow;
import com.mailvault.attachmentworker.storage.AttachmentObjectStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.mockito.Mockito.never;
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
        AttachmentRow attachment = new AttachmentRow(
                attachmentId,
                "user-123",
                "invoice.txt",
                "users/user-123/pending-attachments/%s/invoice.txt".formatted(attachmentId),
                5
        );
        when(attachmentRepository.markProcessing(attachmentId)).thenReturn(true);
        when(objectStorage.readBytes(attachment.objectKey())).thenReturn("hello".getBytes(StandardCharsets.UTF_8));

        attachmentProcessingService.process(attachment);

        verify(attachmentRepository).markReady(
                attachmentId,
                "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"
        );
        verify(attachmentRepository, never()).markFailed(attachmentId);
    }

    @Test
    void processSkipsAttachmentIfClaimFails() {
        UUID attachmentId = UUID.randomUUID();
        AttachmentRow attachment = new AttachmentRow(attachmentId, "user-123", "invoice.txt", "object-key", 5);
        when(attachmentRepository.markProcessing(attachmentId)).thenReturn(false);

        attachmentProcessingService.process(attachment);

        verify(objectStorage, never()).readBytes(attachment.objectKey());
        verify(attachmentRepository, never()).markReady(attachmentId, "");
    }
}
