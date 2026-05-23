package com.mailvault.mailbox.service;

import com.mailvault.mailbox.repository.AttachmentRow;
import com.mailvault.mailbox.repository.EmailHeaderRow;
import com.mailvault.mailbox.repository.EmailMessageRepository;
import com.mailvault.mailbox.repository.InboxRow;
import com.mailvault.mailbox.repository.RecipientRow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailboxQueryServiceTest {

    @Mock
    private EmailMessageRepository emailMessageRepository;

    @InjectMocks
    private MailboxQueryService mailboxQueryService;

    @Test
    void getInboxReturnsNewestMessagesWithAttachmentCount() {
        UUID emailId = UUID.randomUUID();
        when(emailMessageRepository.findInbox("user-123", 10))
                .thenReturn(List.of(new InboxRow(
                        emailId,
                        "billing@example.com",
                        "Invoice",
                        "RECEIVED",
                        Instant.parse("2026-05-23T07:30:00Z"),
                        512L,
                        2
                )));

        var inbox = mailboxQueryService.getInbox("user-123", 10);

        assertThat(inbox).hasSize(1);
        assertThat(inbox.getFirst().id()).isEqualTo(emailId);
        assertThat(inbox.getFirst().sender()).isEqualTo("billing@example.com");
        assertThat(inbox.getFirst().status()).isEqualTo("RECEIVED");
        assertThat(inbox.getFirst().attachmentCount()).isEqualTo(2);
    }

    @Test
    void getEmailDetailReturnsRecipientsAndAttachments() {
        UUID emailId = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();
        when(emailMessageRepository.findHeader(emailId, "user-123"))
                .thenReturn(Optional.of(new EmailHeaderRow(
                        emailId,
                        "user-123",
                        "sender@example.com",
                        "Status update",
                        "RECEIVED",
                        Instant.parse("2026-05-23T08:00:00Z"),
                        2048L
                )));
        when(emailMessageRepository.findRecipients(emailId))
                .thenReturn(List.of(new RecipientRow("abhinav@example.com", "TO")));
        when(emailMessageRepository.findAttachments(emailId))
                .thenReturn(List.of(new AttachmentRow(
                        attachmentId,
                        "report.pdf",
                        "application/pdf",
                        1024L,
                        "READY"
                )));

        var detail = mailboxQueryService.getEmailDetail("user-123", emailId);

        assertThat(detail.id()).isEqualTo(emailId);
        assertThat(detail.status()).isEqualTo("RECEIVED");
        assertThat(detail.recipients()).extracting("address").containsExactly("abhinav@example.com");
        assertThat(detail.attachments()).extracting("filename").containsExactly("report.pdf");
    }

    @Test
    void getEmailDetailReturnsNotFoundForDifferentUserOrMissingEmail() {
        UUID emailId = UUID.randomUUID();
        when(emailMessageRepository.findHeader(emailId, "user-123")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mailboxQueryService.getEmailDetail("user-123", emailId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND");
    }

}
