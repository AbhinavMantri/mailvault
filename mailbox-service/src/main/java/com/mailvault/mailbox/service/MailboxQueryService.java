package com.mailvault.mailbox.service;

import com.mailvault.mailbox.api.AttachmentResponse;
import com.mailvault.mailbox.api.EmailDetailResponse;
import com.mailvault.mailbox.api.InboxItemResponse;
import com.mailvault.mailbox.api.RecipientResponse;
import com.mailvault.mailbox.api.ThreadDetailResponse;
import com.mailvault.mailbox.api.ThreadLabelResponse;
import com.mailvault.mailbox.api.ThreadMessageResponse;
import com.mailvault.mailbox.api.ThreadSummaryResponse;
import com.mailvault.mailbox.repository.AttachmentRow;
import com.mailvault.mailbox.repository.EmailHeaderRow;
import com.mailvault.mailbox.repository.EmailMessageRepository;
import com.mailvault.mailbox.repository.InboxRow;
import com.mailvault.mailbox.repository.RecipientRow;
import com.mailvault.mailbox.repository.ThreadMessageRow;
import com.mailvault.mailbox.repository.ThreadLabelRow;
import com.mailvault.mailbox.repository.ThreadSummaryRow;
import com.mailvault.mailbox.storage.EmailBodyStorage;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@Transactional(readOnly = true)
public class MailboxQueryService {

    private static final Pattern LABEL_PATTERN = Pattern.compile("[A-Z0-9_-]{1,40}");

    private final EmailMessageRepository emailMessageRepository;
    private final EmailBodyStorage emailBodyStorage;

    public MailboxQueryService(EmailMessageRepository emailMessageRepository, EmailBodyStorage emailBodyStorage) {
        this.emailMessageRepository = emailMessageRepository;
        this.emailBodyStorage = emailBodyStorage;
    }

    public List<InboxItemResponse> getInbox(String userId, int limit) {
        return emailMessageRepository.findInbox(userId, limit)
                .stream()
                .map(this::toInboxItem)
                .toList();
    }

    public List<ThreadSummaryResponse> getThreads(String userId, String folder, int limit) {
        List<ThreadSummaryRow> threads = emailMessageRepository.findThreads(userId, folder, limit);
        return toThreadSummaries(userId, threads);
    }

    public List<ThreadSummaryResponse> getThreadsByLabel(String userId, String label, int limit) {
        List<ThreadSummaryRow> threads = emailMessageRepository.findThreadsByLabel(userId, normalizeLabel(label), limit);
        return toThreadSummaries(userId, threads);
    }

    public ThreadDetailResponse getThreadDetail(String userId, UUID threadId) {
        ThreadSummaryRow thread = emailMessageRepository.findThread(threadId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "thread not found"));
        List<ThreadMessageResponse> messages = emailMessageRepository.findThreadMessages(threadId, userId).stream()
                .map(this::toThreadMessage)
                .toList();
        List<ThreadLabelRow> labels = emailMessageRepository.findThreadLabels(threadId, userId);
        if (labels == null) {
            labels = List.of();
        }

        return new ThreadDetailResponse(
                thread.id(),
                userId,
                thread.subject(),
                thread.folder(),
                thread.lastMessageAt(),
                thread.messageCount(),
                thread.unreadCount(),
                labels.stream().map(this::toThreadLabel).toList(),
                messages
        );
    }

    public EmailDetailResponse getEmailDetail(String userId, UUID emailId) {
        EmailHeaderRow email = emailMessageRepository.findHeader(emailId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "email not found"));

        List<RecipientResponse> recipients = emailMessageRepository.findRecipients(emailId).stream()
                .map(this::toRecipient)
                .toList();

        List<AttachmentResponse> attachments = emailMessageRepository.findAttachments(emailId).stream()
                .map(this::toAttachment)
                .toList();
        String textBody = emailBodyStorage.readText(email.textObjectKey());
        String htmlBody = emailBodyStorage.readText(email.htmlObjectKey());

        return new EmailDetailResponse(
                email.id(),
                email.userId(),
                email.sender(),
                email.subject(),
                textBody,
                htmlBody,
                email.status(),
                email.receivedAt(),
                email.logicalSizeBytes(),
                recipients,
                attachments
        );
    }

    private InboxItemResponse toInboxItem(InboxRow email) {
        return new InboxItemResponse(
                email.id(),
                email.sender(),
                email.subject(),
                email.status(),
                email.receivedAt(),
                email.logicalSizeBytes(),
                email.attachmentCount()
        );
    }

    private List<ThreadSummaryResponse> toThreadSummaries(String userId, List<ThreadSummaryRow> threads) {
        Map<UUID, List<ThreadLabelRow>> fetchedLabelsByThread = emailMessageRepository.findLabelsForThreads(
                threads.stream().map(ThreadSummaryRow::id).toList(),
                userId
        );
        Map<UUID, List<ThreadLabelRow>> labelsByThread = fetchedLabelsByThread == null ? Map.of() : fetchedLabelsByThread;
        return threads.stream()
                .map(thread -> toThreadSummary(thread, labelsByThread.getOrDefault(thread.id(), List.of())))
                .toList();
    }

    private ThreadSummaryResponse toThreadSummary(ThreadSummaryRow thread, List<ThreadLabelRow> labels) {
        return new ThreadSummaryResponse(
                thread.id(),
                thread.subject(),
                thread.folder(),
                thread.lastSender(),
                thread.lastMessageAt(),
                thread.messageCount(),
                thread.unreadCount(),
                thread.attachmentCount(),
                labels.stream().map(this::toThreadLabel).toList()
        );
    }

    private ThreadMessageResponse toThreadMessage(ThreadMessageRow message) {
        return new ThreadMessageResponse(
                message.emailId(),
                message.direction(),
                message.sender(),
                message.subject(),
                emailBodyStorage.readText(message.textObjectKey()),
                emailBodyStorage.readText(message.htmlObjectKey()),
                message.receivedAt(),
                message.logicalSizeBytes(),
                emailMessageRepository.findRecipients(message.emailId()).stream()
                        .map(this::toRecipient)
                        .toList(),
                emailMessageRepository.findAttachments(message.emailId()).stream()
                        .map(this::toAttachment)
                        .toList()
        );
    }

    private RecipientResponse toRecipient(RecipientRow recipient) {
        return new RecipientResponse(
                recipient.address(),
                recipient.type()
        );
    }

    private AttachmentResponse toAttachment(AttachmentRow attachment) {
        return new AttachmentResponse(
                attachment.id(),
                attachment.filename(),
                attachment.contentType(),
                attachment.sizeBytes(),
                attachment.status()
        );
    }

    private ThreadLabelResponse toThreadLabel(ThreadLabelRow label) {
        return new ThreadLabelResponse(
                label.label(),
                label.source(),
                label.confidenceScore()
        );
    }

    private String normalizeLabel(String label) {
        String normalized = label.trim().toUpperCase(Locale.ROOT);
        if (!LABEL_PATTERN.matcher(normalized).matches()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "label must be 1-40 characters using letters, numbers, underscore, or hyphen"
            );
        }
        return normalized;
    }
}
