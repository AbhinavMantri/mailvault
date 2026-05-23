package com.mailvault.ingestion.service;

import com.mailvault.ingestion.api.dto.EmailImportRequest;
import com.mailvault.ingestion.domain.Attachment;
import com.mailvault.ingestion.domain.AttachmentStatus;
import com.mailvault.ingestion.domain.EmailMessage;
import com.mailvault.ingestion.domain.StorageUsage;
import com.mailvault.ingestion.events.EmailEventPublisher;
import com.mailvault.ingestion.events.EmailReceivedEvent;
import com.mailvault.ingestion.repository.AttachmentRepository;
import com.mailvault.ingestion.repository.EmailMessageRepository;
import com.mailvault.ingestion.repository.StorageUsageRepository;
import com.mailvault.ingestion.storage.ObjectStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailIngestionServiceTest {

    @Mock
    private ObjectStorageService objectStorageService;

    @Mock
    private EmailMessageRepository emailMessageRepository;

    @Mock
    private AttachmentRepository attachmentRepository;

    @Mock
    private StorageUsageRepository storageUsageRepository;

    @Mock
    private EmailEventPublisher emailEventPublisher;

    @InjectMocks
    private EmailIngestionService emailIngestionService;

    @Test
    void importEmailWithoutAttachmentsPersistsMetadataAndPublishesEvent() {
        EmailImportRequest request = new EmailImportRequest(
                "user-123",
                "billing@example.com",
                List.of("abhinav@example.com"),
                "Invoice for May",
                "Invoice attached.",
                "<p>Invoice attached.</p>",
                List.of()
        );
        when(storageUsageRepository.findById("user-123")).thenReturn(Optional.empty());

        var response = emailIngestionService.importEmail(request);

        assertThat(response.status()).isEqualTo("ACCEPTED");
        assertThat(response.logicalSizeBytes()).isPositive();
        verify(objectStorageService).putText(any(), any(), eq("message/rfc822"));
        verify(objectStorageService).putText(any(), eq("Invoice attached."), eq("text/plain"));
        verify(objectStorageService).putText(any(), eq("<p>Invoice attached.</p>"), eq("text/html"));
        verify(storageUsageRepository).save(any(StorageUsage.class));
        verify(emailMessageRepository).save(any(EmailMessage.class));
        verify(emailEventPublisher).publishEmailReceived(any(EmailReceivedEvent.class));
    }

    @Test
    void importEmailWithUploadedAttachmentLinksAttachmentAndCountsSize() {
        UUID attachmentId = UUID.randomUUID();
        Attachment attachment = uploadedAttachment(attachmentId, 512);
        EmailImportRequest request = new EmailImportRequest(
                "user-123",
                "billing@example.com",
                List.of("abhinav@example.com"),
                "Invoice for May",
                "Body",
                null,
                List.of(attachmentId)
        );
        when(storageUsageRepository.findById("user-123")).thenReturn(Optional.empty());
        when(attachmentRepository.findByIdInAndUserIdAndStatus(
                List.of(attachmentId),
                "user-123",
                AttachmentStatus.UPLOADED
        )).thenReturn(List.of(attachment));

        var response = emailIngestionService.importEmail(request);

        assertThat(response.logicalSizeBytes()).isEqualTo("Body".length() + 512);
        verify(emailMessageRepository).save(any(EmailMessage.class));
        verify(emailEventPublisher).publishEmailReceived(any(EmailReceivedEvent.class));
    }

    @Test
    void importEmailRejectsAttachmentThatIsNotUploadedForUser() {
        UUID attachmentId = UUID.randomUUID();
        EmailImportRequest request = new EmailImportRequest(
                "user-123",
                "billing@example.com",
                List.of("abhinav@example.com"),
                "Invoice for May",
                "Body",
                null,
                List.of(attachmentId)
        );
        when(attachmentRepository.findByIdInAndUserIdAndStatus(
                List.of(attachmentId),
                "user-123",
                AttachmentStatus.UPLOADED
        )).thenReturn(List.of());

        assertThatThrownBy(() -> emailIngestionService.importEmail(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("All attachments must be uploaded before email import");
        verify(emailMessageRepository, never()).save(any());
        verify(emailEventPublisher, never()).publishEmailReceived(any());
    }

    @Test
    void importEmailRejectsQuotaExceededBeforeWritingObjects() {
        EmailImportRequest request = new EmailImportRequest(
                "user-123",
                "billing@example.com",
                List.of("abhinav@example.com"),
                "Invoice for May",
                "Body",
                null,
                List.of()
        );
        StorageUsage usage = new StorageUsage("user-123", 5L * 1024 * 1024 * 1024, 5L * 1024 * 1024 * 1024, Instant.now());
        when(storageUsageRepository.findById("user-123")).thenReturn(Optional.of(usage));

        assertThatThrownBy(() -> emailIngestionService.importEmail(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User storage quota exceeded");

        verify(objectStorageService, never()).putText(any(), any(), any());
        verify(emailMessageRepository, never()).save(any());
    }

    private Attachment uploadedAttachment(UUID attachmentId, long sizeBytes) {
        return new Attachment(
                attachmentId,
                "user-123",
                "invoice.txt",
                "users/user-123/pending-attachments/%s/invoice.txt".formatted(attachmentId),
                "text/plain",
                sizeBytes,
                AttachmentStatus.UPLOADED,
                Instant.now()
        );
    }
}

