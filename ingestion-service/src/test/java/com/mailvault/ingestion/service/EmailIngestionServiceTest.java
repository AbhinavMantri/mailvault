package com.mailvault.ingestion.service;

import com.mailvault.ingestion.api.dto.EmailDraftRequest;
import com.mailvault.ingestion.api.dto.EmailForwardRequest;
import com.mailvault.ingestion.api.dto.EmailImportRequest;
import com.mailvault.ingestion.api.dto.EmailReplyRequest;
import com.mailvault.ingestion.api.dto.SendDraftRequest;
import com.mailvault.ingestion.domain.Attachment;
import com.mailvault.ingestion.domain.AttachmentStatus;
import com.mailvault.ingestion.domain.EmailMessage;
import com.mailvault.ingestion.domain.EmailAttachmentRef;
import com.mailvault.ingestion.domain.EmailRecipient;
import com.mailvault.ingestion.domain.EmailStatus;
import com.mailvault.ingestion.domain.MessageDirection;
import com.mailvault.ingestion.domain.RecipientType;
import com.mailvault.ingestion.domain.ThreadFolder;
import com.mailvault.ingestion.domain.ThreadMessage;
import com.mailvault.ingestion.domain.MailboxThread;
import com.mailvault.ingestion.events.EmailReceivedEvent;
import com.mailvault.ingestion.outbox.OutboxEventService;
import com.mailvault.ingestion.repository.AttachmentRepository;
import com.mailvault.ingestion.repository.EmailMessageRepository;
import com.mailvault.ingestion.repository.ThreadMessageRepository;
import com.mailvault.ingestion.repository.MailboxThreadRepository;
import com.mailvault.ingestion.storage.ObjectStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
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
    private OutboxEventService outboxEventService;

    @Mock
    private MailboxThreadRepository mailboxThreadRepository;

    @Mock
    private ThreadMessageRepository threadMessageRepository;

    @InjectMocks
    private EmailIngestionService emailIngestionService;

    @Test
    void importEmailWithoutAttachmentsPersistsMetadataAndWritesOutboxEvent() {
        EmailImportRequest request = new EmailImportRequest(
                "user-123",
                "billing@example.com",
                List.of("abhinav@example.com"),
                List.of("manager@example.com"),
                List.of("audit@example.com"),
                "Invoice for May",
                "Invoice attached.",
                "<p>Invoice attached.</p>",
                List.of()
        );
        var response = emailIngestionService.importEmail(request);

        assertThat(response.status()).isEqualTo("ACCEPTED");
        assertThat(response.logicalSizeBytes()).isPositive();
        verify(objectStorageService).putText(any(), any(), eq("message/rfc822"));
        verify(objectStorageService).putText(any(), eq("Invoice attached."), eq("text/plain"));
        verify(objectStorageService).putText(any(), eq("<p>Invoice attached.</p>"), eq("text/html"));
        verify(emailMessageRepository).save(any(EmailMessage.class));
        verify(outboxEventService).saveEmailReceived(any(EmailReceivedEvent.class), any(Instant.class));
    }

    @Test
    void importEmailWithUploadedAttachmentLinksAttachmentCountsSizeAndWritesOutboxEvent() {
        UUID attachmentId = UUID.randomUUID();
        Attachment attachment = uploadedAttachment(attachmentId, 512);
        EmailImportRequest request = new EmailImportRequest(
                "user-123",
                "billing@example.com",
                List.of("abhinav@example.com"),
                List.of(),
                List.of(),
                "Invoice for May",
                "Body",
                null,
                List.of(attachmentId)
        );
        when(attachmentRepository.findByIdInAndUserIdAndStatusIn(
                List.of(attachmentId),
                "user-123",
                List.of(AttachmentStatus.UPLOADED, AttachmentStatus.PROCESSING, AttachmentStatus.READY)
        )).thenReturn(List.of(attachment));

        var response = emailIngestionService.importEmail(request);

        assertThat(response.logicalSizeBytes()).isEqualTo("Body".length() + 512);
        verify(emailMessageRepository).save(any(EmailMessage.class));
        verify(outboxEventService).saveEmailReceived(any(EmailReceivedEvent.class), any(Instant.class));
    }

    @Test
    void importEmailRejectsAttachmentThatIsNotUploadedForUser() {
        UUID attachmentId = UUID.randomUUID();
        EmailImportRequest request = new EmailImportRequest(
                "user-123",
                "billing@example.com",
                List.of("abhinav@example.com"),
                List.of(),
                List.of(),
                "Invoice for May",
                "Body",
                null,
                List.of(attachmentId)
        );
        when(attachmentRepository.findByIdInAndUserIdAndStatusIn(
                List.of(attachmentId),
                "user-123",
                List.of(AttachmentStatus.UPLOADED, AttachmentStatus.PROCESSING, AttachmentStatus.READY)
        )).thenReturn(List.of());

        assertThatThrownBy(() -> emailIngestionService.importEmail(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("All attachments must be uploaded before email import");
        verify(emailMessageRepository, never()).save(any());
        verify(outboxEventService, never()).saveEmailReceived(any(), any());
    }

    @Test
    void replyToThreadPersistsMessageLinksExistingThreadAndWritesOutboxEvent() {
        UUID threadId = UUID.randomUUID();
        MailboxThread thread = new MailboxThread(
                threadId,
                "user-123",
                "invoice for may",
                ThreadFolder.INBOX,
                Instant.now(),
                "billing@example.com",
                1,
                1,
                Instant.now(),
                Instant.now()
        );
        EmailReplyRequest request = new EmailReplyRequest(
                "user-123",
                "abhinav@example.com",
                List.of("billing@example.com"),
                List.of(),
                List.of(),
                "Re: Invoice for May",
                "Thanks, received.",
                null,
                List.of()
        );
        when(mailboxThreadRepository.findByIdAndUserId(threadId, "user-123")).thenReturn(java.util.Optional.of(thread));

        var response = emailIngestionService.replyToThread(threadId, request);

        assertThat(response.status()).isEqualTo("ACCEPTED");
        assertThat(response.logicalSizeBytes()).isEqualTo("Thanks, received.".length());
        verify(emailMessageRepository).save(any(EmailMessage.class));
        verify(threadMessageRepository).save(any());
        verify(outboxEventService).saveEmailReceived(any(EmailReceivedEvent.class), any(Instant.class));
    }

    @Test
    void replyToThreadRejectsThreadThatDoesNotBelongToUser() {
        UUID threadId = UUID.randomUUID();
        EmailReplyRequest request = new EmailReplyRequest(
                "user-123",
                "abhinav@example.com",
                List.of("billing@example.com"),
                List.of(),
                List.of(),
                "Re: Invoice for May",
                "Thanks, received.",
                null,
                List.of()
        );
        when(mailboxThreadRepository.findByIdAndUserId(threadId, "user-123")).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> emailIngestionService.replyToThread(threadId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Thread not found");
        verify(emailMessageRepository, never()).save(any());
        verify(outboxEventService, never()).saveEmailReceived(any(), any());
    }

    @Test
    void createDraftPersistsDraftThreadWithoutOutboxEvent() {
        EmailDraftRequest request = new EmailDraftRequest(
                "user-123",
                "abhinav@example.com",
                List.of("billing@example.com"),
                List.of(),
                List.of(),
                "",
                "Draft body",
                null,
                List.of()
        );

        var response = emailIngestionService.createDraft(request);

        assertThat(response.status()).isEqualTo("DRAFT");
        assertThat(response.logicalSizeBytes()).isEqualTo("Draft body".length());
        verify(emailMessageRepository).save(any(EmailMessage.class));
        verify(mailboxThreadRepository).save(any(MailboxThread.class));
        verify(threadMessageRepository).save(any());
        verify(outboxEventService, never()).saveEmailReceived(any(), any());
    }

    @Test
    void sendDraftTransitionsExistingDraftToOutboundAndWritesOutboxEvent() {
        UUID emailId = UUID.randomUUID();
        MailboxThread thread = new MailboxThread(
                UUID.randomUUID(),
                "user-123",
                "invoice draft",
                ThreadFolder.DRAFT,
                Instant.now(),
                "abhinav@example.com",
                1,
                0,
                Instant.now(),
                Instant.now()
        );
        EmailMessage email = new EmailMessage(
                emailId,
                "user-123",
                "abhinav@example.com",
                "Invoice draft",
                "raw-key",
                "text-key",
                null,
                128L,
                EmailStatus.DRAFT,
                Instant.now()
        );
        email.addRecipient(new EmailRecipient("billing@example.com", RecipientType.TO));
        ThreadMessage threadMessage = new ThreadMessage(
                UUID.randomUUID(),
                thread,
                email,
                MessageDirection.DRAFT,
                Instant.now(),
                Instant.now()
        );
        when(threadMessageRepository.findByEmailIdAndEmailUserId(emailId, "user-123"))
                .thenReturn(java.util.Optional.of(threadMessage));

        var response = emailIngestionService.sendDraft(emailId, new SendDraftRequest("user-123"));

        assertThat(response.status()).isEqualTo("ACCEPTED");
        assertThat(response.logicalSizeBytes()).isEqualTo(128L);
        assertThat(email.getStatus()).isEqualTo(EmailStatus.INDEX_PENDING);
        verify(outboxEventService).saveEmailReceived(any(EmailReceivedEvent.class), any(Instant.class));
    }

    @Test
    void sendDraftRejectsMissingDraft() {
        UUID emailId = UUID.randomUUID();
        when(threadMessageRepository.findByEmailIdAndEmailUserId(emailId, "user-123"))
                .thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> emailIngestionService.sendDraft(emailId, new SendDraftRequest("user-123")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Draft not found");
        verify(outboxEventService, never()).saveEmailReceived(any(), any());
    }

    @Test
    void forwardEmailCreatesNewOutboundThreadAndReusesAttachmentsWhenRequested() {
        UUID originalEmailId = UUID.randomUUID();
        Attachment attachment = uploadedAttachment(UUID.randomUUID(), 512);
        EmailMessage originalEmail = new EmailMessage(
                originalEmailId,
                "user-123",
                "billing@example.com",
                "Invoice for May",
                "raw-key",
                "text-key",
                null,
                128L,
                EmailStatus.INDEX_PENDING,
                Instant.now()
        );
        originalEmail.addAttachmentRef(new EmailAttachmentRef(attachment));
        EmailForwardRequest request = new EmailForwardRequest(
                "user-123",
                "abhinav@example.com",
                List.of("finance@example.com"),
                List.of(),
                List.of(),
                null,
                "Forwarding this invoice.",
                null,
                true,
                List.of()
        );
        when(emailMessageRepository.findByIdAndUserId(originalEmailId, "user-123"))
                .thenReturn(java.util.Optional.of(originalEmail));
        when(objectStorageService.readText("text-key")).thenReturn("Original invoice body.");

        var response = emailIngestionService.forwardEmail(originalEmailId, request);

        assertThat(response.status()).isEqualTo("ACCEPTED");
        assertThat(response.logicalSizeBytes()).isGreaterThan("Forwarding this invoice.".length() + 512);
        verify(objectStorageService).putText(any(), org.mockito.ArgumentMatchers.contains("Forwarded message"), eq("text/plain"));
        verify(emailMessageRepository).save(any(EmailMessage.class));
        verify(mailboxThreadRepository).save(any(MailboxThread.class));
        verify(threadMessageRepository).save(any(ThreadMessage.class));
        verify(outboxEventService).saveEmailReceived(any(EmailReceivedEvent.class), any(Instant.class));
    }

    @Test
    void forwardEmailRejectsMissingOriginalEmail() {
        UUID originalEmailId = UUID.randomUUID();
        EmailForwardRequest request = new EmailForwardRequest(
                "user-123",
                "abhinav@example.com",
                List.of("finance@example.com"),
                List.of(),
                List.of(),
                null,
                "Forwarding this invoice.",
                null,
                false,
                List.of()
        );
        when(emailMessageRepository.findByIdAndUserId(originalEmailId, "user-123"))
                .thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> emailIngestionService.forwardEmail(originalEmailId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email not found");
        verify(emailMessageRepository, never()).save(any());
        verify(outboxEventService, never()).saveEmailReceived(any(), any());
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
