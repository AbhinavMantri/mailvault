package com.mailvault.mailbox.service;

import com.mailvault.mailbox.domain.Attachment;
import com.mailvault.mailbox.domain.AttachmentStatus;
import com.mailvault.mailbox.domain.EmailAttachmentRef;
import com.mailvault.mailbox.domain.EmailMessage;
import com.mailvault.mailbox.domain.EmailRecipient;
import com.mailvault.mailbox.domain.EmailStatus;
import com.mailvault.mailbox.domain.RecipientType;
import com.mailvault.mailbox.domain.StorageUsage;
import com.mailvault.mailbox.repository.EmailMessageRepository;
import com.mailvault.mailbox.repository.StorageUsageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailboxQueryServiceTest {

    @Mock
    private EmailMessageRepository emailMessageRepository;

    @Mock
    private StorageUsageRepository storageUsageRepository;

    @InjectMocks
    private MailboxQueryService mailboxQueryService;

    @Test
    void getInboxReturnsNewestMessagesWithAttachmentCount() {
        EmailMessage email = mockEmail(UUID.randomUUID(), "billing@example.com", "Invoice", Instant.parse("2026-05-23T07:30:00Z"));
        when(email.getAttachmentRefs()).thenReturn(List.of(mock(EmailAttachmentRef.class), mock(EmailAttachmentRef.class)));
        when(emailMessageRepository.findByUserIdOrderByReceivedAtDesc("user-123", PageRequest.of(0, 10)))
                .thenReturn(List.of(email));

        var inbox = mailboxQueryService.getInbox("user-123", 10);

        assertThat(inbox).hasSize(1);
        assertThat(inbox.getFirst().sender()).isEqualTo("billing@example.com");
        assertThat(inbox.getFirst().attachmentCount()).isEqualTo(2);
    }

    @Test
    void getEmailDetailReturnsRecipientsAndAttachments() {
        UUID emailId = UUID.randomUUID();
        EmailMessage email = mockEmail(emailId, "sender@example.com", "Status update", Instant.parse("2026-05-23T08:00:00Z"));
        EmailRecipient recipient = mock(EmailRecipient.class);
        when(recipient.getRecipientAddress()).thenReturn("abhinav@example.com");
        when(recipient.getRecipientType()).thenReturn(RecipientType.TO);

        Attachment attachment = mock(Attachment.class);
        UUID attachmentId = UUID.randomUUID();
        when(attachment.getId()).thenReturn(attachmentId);
        when(attachment.getFilename()).thenReturn("report.pdf");
        when(attachment.getContentType()).thenReturn("application/pdf");
        when(attachment.getSizeBytes()).thenReturn(2048L);
        when(attachment.getStatus()).thenReturn(AttachmentStatus.READY);

        EmailAttachmentRef ref = mock(EmailAttachmentRef.class);
        when(ref.getAttachment()).thenReturn(attachment);
        when(email.getRecipients()).thenReturn(List.of(recipient));
        when(email.getAttachmentRefs()).thenReturn(List.of(ref));
        when(emailMessageRepository.findByIdAndUserId(emailId, "user-123")).thenReturn(Optional.of(email));

        var detail = mailboxQueryService.getEmailDetail("user-123", emailId);

        assertThat(detail.id()).isEqualTo(emailId);
        assertThat(detail.recipients()).extracting("address").containsExactly("abhinav@example.com");
        assertThat(detail.attachments()).extracting("filename").containsExactly("report.pdf");
    }

    @Test
    void getEmailDetailReturnsNotFoundForDifferentUserOrMissingEmail() {
        UUID emailId = UUID.randomUUID();
        when(emailMessageRepository.findByIdAndUserId(emailId, "user-123")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mailboxQueryService.getEmailDetail("user-123", emailId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND");
    }

    @Test
    void getStorageUsageCalculatesUsedPercentage() {
        StorageUsage usage = mock(StorageUsage.class);
        Instant updatedAt = Instant.parse("2026-05-23T09:00:00Z");
        when(usage.getUserId()).thenReturn("user-123");
        when(usage.getUsedBytes()).thenReturn(25L);
        when(usage.getQuotaBytes()).thenReturn(100L);
        when(usage.getUpdatedAt()).thenReturn(updatedAt);
        when(storageUsageRepository.findById("user-123")).thenReturn(Optional.of(usage));

        var response = mailboxQueryService.getStorageUsage("user-123");

        assertThat(response.usedBytes()).isEqualTo(25L);
        assertThat(response.quotaBytes()).isEqualTo(100L);
        assertThat(response.usedPercent()).isEqualTo(25.0);
        assertThat(response.updatedAt()).isEqualTo(updatedAt);
    }

    private EmailMessage mockEmail(UUID id, String sender, String subject, Instant receivedAt) {
        EmailMessage email = mock(EmailMessage.class);
        when(email.getId()).thenReturn(id);
        lenient().when(email.getUserId()).thenReturn("user-123");
        when(email.getSender()).thenReturn(sender);
        when(email.getSubject()).thenReturn(subject);
        when(email.getStatus()).thenReturn(EmailStatus.RECEIVED);
        when(email.getReceivedAt()).thenReturn(receivedAt);
        when(email.getLogicalSizeBytes()).thenReturn(512L);
        return email;
    }
}
