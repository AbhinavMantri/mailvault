package com.mailvault.mailbox.service;

import com.mailvault.mailbox.api.AttachmentResponse;
import com.mailvault.mailbox.api.EmailDetailResponse;
import com.mailvault.mailbox.api.InboxItemResponse;
import com.mailvault.mailbox.api.RecipientResponse;
import com.mailvault.mailbox.api.ThreadDetailResponse;
import com.mailvault.mailbox.api.ThreadMessageResponse;
import com.mailvault.mailbox.api.ThreadSummaryResponse;
import com.mailvault.mailbox.repository.AttachmentRow;
import com.mailvault.mailbox.repository.EmailHeaderRow;
import com.mailvault.mailbox.repository.EmailMessageRepository;
import com.mailvault.mailbox.repository.InboxRow;
import com.mailvault.mailbox.repository.RecipientRow;
import com.mailvault.mailbox.repository.ThreadMessageRow;
import com.mailvault.mailbox.repository.ThreadSummaryRow;
import com.mailvault.mailbox.storage.EmailBodyStorage;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class MailboxQueryService {

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
        return emailMessageRepository.findThreads(userId, folder, limit)
                .stream()
                .map(this::toThreadSummary)
                .toList();
    }

    public ThreadDetailResponse getThreadDetail(String userId, UUID threadId) {
        ThreadSummaryRow thread = emailMessageRepository.findThread(threadId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "thread not found"));
        List<ThreadMessageResponse> messages = emailMessageRepository.findThreadMessages(threadId).stream()
                .map(this::toThreadMessage)
                .toList();

        return new ThreadDetailResponse(
                thread.id(),
                userId,
                thread.subject(),
                thread.folder(),
                thread.lastMessageAt(),
                thread.messageCount(),
                thread.unreadCount(),
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

    private ThreadSummaryResponse toThreadSummary(ThreadSummaryRow thread) {
        return new ThreadSummaryResponse(
                thread.id(),
                thread.subject(),
                thread.folder(),
                thread.lastSender(),
                thread.lastMessageAt(),
                thread.messageCount(),
                thread.unreadCount(),
                thread.attachmentCount()
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
}
