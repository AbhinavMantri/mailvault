package com.mailvault.ingestion.service;

import com.mailvault.ingestion.api.dto.AttachmentInitiateRequest;
import com.mailvault.ingestion.domain.Attachment;
import com.mailvault.ingestion.domain.AttachmentStatus;
import com.mailvault.ingestion.events.AttachmentUploadedEvent;
import com.mailvault.ingestion.outbox.OutboxEventService;
import com.mailvault.ingestion.repository.AttachmentRepository;
import com.mailvault.ingestion.storage.ObjectStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttachmentUploadServiceTest {

    @Mock
    private AttachmentRepository attachmentRepository;

    @Mock
    private ObjectStorageService objectStorageService;

    @Mock
    private OutboxEventService outboxEventService;

    @InjectMocks
    private AttachmentUploadService attachmentUploadService;

    @Test
    void initiateUploadCreatesPendingAttachmentAndReturnsPresignedUrl() {
        AttachmentInitiateRequest request = new AttachmentInitiateRequest(
                "user-123",
                "invoice may.txt",
                "text/plain",
                128
        );
        when(objectStorageService.presignedPutUrl(any(), any())).thenReturn("http://minio/upload");

        var response = attachmentUploadService.initiateUpload(request);

        ArgumentCaptor<Attachment> attachmentCaptor = ArgumentCaptor.forClass(Attachment.class);
        verify(attachmentRepository).save(attachmentCaptor.capture());
        Attachment savedAttachment = attachmentCaptor.getValue();

        assertThat(response.attachmentId()).isEqualTo(savedAttachment.getId());
        assertThat(response.uploadUrl()).isEqualTo("http://minio/upload");
        assertThat(response.expiresInSeconds()).isEqualTo(900);
        assertThat(response.status()).isEqualTo(AttachmentStatus.PENDING_UPLOAD.name());
        assertThat(savedAttachment.getUserId()).isEqualTo("user-123");
        assertThat(savedAttachment.getFilename()).isEqualTo("invoice may.txt");
        assertThat(savedAttachment.getObjectKey()).contains("invoice_may.txt");
        assertThat(savedAttachment.getStatus()).isEqualTo(AttachmentStatus.PENDING_UPLOAD);
        verify(objectStorageService).presignedPutUrl(eq(savedAttachment.getObjectKey()), any());
    }

    @Test
    void completeUploadMarksPendingAttachmentAsUploaded() {
        UUID attachmentId = UUID.randomUUID();
        Attachment attachment = new Attachment(
                attachmentId,
                "user-123",
                "invoice.txt",
                "users/user-123/pending-attachments/%s/invoice.txt".formatted(attachmentId),
                "text/plain",
                128,
                AttachmentStatus.PENDING_UPLOAD,
                Instant.now()
        );
        when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));

        var response = attachmentUploadService.completeUpload(attachmentId);

        assertThat(response.attachmentId()).isEqualTo(attachmentId);
        assertThat(response.status()).isEqualTo(AttachmentStatus.UPLOADED.name());
        assertThat(attachment.getStatus()).isEqualTo(AttachmentStatus.UPLOADED);
        verify(attachmentRepository).save(attachment);
        verify(outboxEventService).saveAttachmentUploaded(any(AttachmentUploadedEvent.class), any(Instant.class));
    }

    @Test
    void completeUploadRejectsAttachmentThatIsNotPending() {
        UUID attachmentId = UUID.randomUUID();
        Attachment attachment = new Attachment(
                attachmentId,
                "user-123",
                "invoice.txt",
                "users/user-123/pending-attachments/%s/invoice.txt".formatted(attachmentId),
                "text/plain",
                128,
                AttachmentStatus.UPLOADED,
                Instant.now()
        );
        when(attachmentRepository.findById(attachmentId)).thenReturn(Optional.of(attachment));

        assertThatThrownBy(() -> attachmentUploadService.completeUpload(attachmentId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Attachment is not pending upload");
    }
}
