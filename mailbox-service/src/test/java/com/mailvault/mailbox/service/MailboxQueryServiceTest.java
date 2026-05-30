package com.mailvault.mailbox.service;

import com.mailvault.mailbox.repository.AttachmentRow;
import com.mailvault.mailbox.repository.EmailHeaderRow;
import com.mailvault.mailbox.repository.EmailMessageRepository;
import com.mailvault.mailbox.repository.InboxRow;
import com.mailvault.mailbox.repository.RecipientRow;
import com.mailvault.mailbox.repository.ThreadMessageRow;
import com.mailvault.mailbox.repository.ThreadLabelRow;
import com.mailvault.mailbox.repository.ThreadSummaryRow;
import com.mailvault.mailbox.storage.EmailBodyStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailboxQueryServiceTest {

    @Mock
    private EmailMessageRepository emailMessageRepository;

    @Mock
    private EmailBodyStorage emailBodyStorage;

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
                        "users/user-123/emails/%s/body.txt".formatted(emailId),
                        "users/user-123/emails/%s/body.html".formatted(emailId),
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
        when(emailBodyStorage.readText("users/user-123/emails/%s/body.txt".formatted(emailId)))
                .thenReturn("Plain status update");
        when(emailBodyStorage.readText("users/user-123/emails/%s/body.html".formatted(emailId)))
                .thenReturn("<p>Plain status update</p>");

        var detail = mailboxQueryService.getEmailDetail("user-123", emailId);

        assertThat(detail.id()).isEqualTo(emailId);
        assertThat(detail.status()).isEqualTo("RECEIVED");
        assertThat(detail.textBody()).isEqualTo("Plain status update");
        assertThat(detail.htmlBody()).isEqualTo("<p>Plain status update</p>");
        assertThat(detail.recipients()).extracting("address").containsExactly("abhinav@example.com");
        assertThat(detail.attachments()).extracting("filename").containsExactly("report.pdf");
    }

    @Test
    void getThreadsReturnsThreadSummaries() {
        UUID threadId = UUID.randomUUID();
        when(emailMessageRepository.findThreads("user-123", "INBOX", 10))
                .thenReturn(List.of(new ThreadSummaryRow(
                        threadId,
                        "Invoice",
                        "INBOX",
                        "billing@example.com",
                        Instant.parse("2026-05-23T08:00:00Z"),
                        2,
                        1,
                        1
        )));
        when(emailMessageRepository.findLabelsForThreads(List.of(threadId), "user-123"))
                .thenReturn(Map.of(threadId, List.of(new ThreadLabelRow("IMPORTANT", "USER", null))));

        var threads = mailboxQueryService.getThreads("user-123", "INBOX", 10);

        assertThat(threads).hasSize(1);
        assertThat(threads.getFirst().id()).isEqualTo(threadId);
        assertThat(threads.getFirst().folder()).isEqualTo("INBOX");
        assertThat(threads.getFirst().messageCount()).isEqualTo(2);
        assertThat(threads.getFirst().labels()).extracting("label").containsExactly("IMPORTANT");
        assertThat(threads.getFirst().labels()).extracting("source").containsExactly("USER");
    }

    @Test
    void getThreadsByLabelNormalizesLabelAndReturnsThreadSummaries() {
        UUID threadId = UUID.randomUUID();
        when(emailMessageRepository.findThreadsByLabel("user-123", "FAVORITE", 10))
                .thenReturn(List.of(new ThreadSummaryRow(
                        threadId,
                        "Invoice",
                        "INBOX",
                        "billing@example.com",
                        Instant.parse("2026-05-23T08:00:00Z"),
                        2,
                        1,
                        1
        )));
        when(emailMessageRepository.findLabelsForThreads(List.of(threadId), "user-123"))
                .thenReturn(Map.of(threadId, List.of(new ThreadLabelRow("FAVORITE", "USER", null))));

        var threads = mailboxQueryService.getThreadsByLabel("user-123", "favorite", 10);

        assertThat(threads).hasSize(1);
        assertThat(threads.getFirst().id()).isEqualTo(threadId);
        assertThat(threads.getFirst().labels()).extracting("label").containsExactly("FAVORITE");
        assertThat(threads.getFirst().labels()).extracting("source").containsExactly("USER");
    }

    @Test
    void getThreadDetailReturnsMessagesWithBodiesRecipientsAndAttachments() {
        UUID threadId = UUID.randomUUID();
        UUID emailId = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();
        when(emailMessageRepository.findThread(threadId, "user-123"))
                .thenReturn(Optional.of(new ThreadSummaryRow(
                        threadId,
                        "Invoice",
                        "INBOX",
                        "billing@example.com",
                        Instant.parse("2026-05-23T08:00:00Z"),
                        1,
                        1,
                        1
                )));
        when(emailMessageRepository.findThreadMessages(threadId))
                .thenReturn(List.of(new ThreadMessageRow(
                        threadId,
                        emailId,
                        "INBOUND",
                        "billing@example.com",
                        "Invoice",
                        "users/user-123/emails/%s/body.txt".formatted(emailId),
                        null,
                        Instant.parse("2026-05-23T08:00:00Z"),
                        2048L
        )));
        when(emailMessageRepository.findThreadLabels(threadId, "user-123"))
                .thenReturn(List.of(new ThreadLabelRow("IMPORTANT", "USER", null)));
        when(emailMessageRepository.findRecipients(emailId))
                .thenReturn(List.of(
                        new RecipientRow("abhinav@example.com", "TO"),
                        new RecipientRow("manager@example.com", "CC")
                ));
        when(emailMessageRepository.findAttachments(emailId))
                .thenReturn(List.of(new AttachmentRow(
                        attachmentId,
                        "invoice.pdf",
                        "application/pdf",
                        1024L,
                        "READY"
                )));
        when(emailBodyStorage.readText("users/user-123/emails/%s/body.txt".formatted(emailId)))
                .thenReturn("Invoice attached");

        var detail = mailboxQueryService.getThreadDetail("user-123", threadId);

        assertThat(detail.id()).isEqualTo(threadId);
        assertThat(detail.labels()).extracting("label").containsExactly("IMPORTANT");
        assertThat(detail.labels()).extracting("source").containsExactly("USER");
        assertThat(detail.messages()).hasSize(1);
        assertThat(detail.messages().getFirst().direction()).isEqualTo("INBOUND");
        assertThat(detail.messages().getFirst().textBody()).isEqualTo("Invoice attached");
        assertThat(detail.messages().getFirst().recipients()).extracting("type").containsExactly("TO", "CC");
        assertThat(detail.messages().getFirst().attachments()).extracting("filename").containsExactly("invoice.pdf");
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
