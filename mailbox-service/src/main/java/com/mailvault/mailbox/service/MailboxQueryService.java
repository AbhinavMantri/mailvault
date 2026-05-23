package com.mailvault.mailbox.service;

import com.mailvault.mailbox.api.AttachmentResponse;
import com.mailvault.mailbox.api.EmailDetailResponse;
import com.mailvault.mailbox.api.InboxItemResponse;
import com.mailvault.mailbox.api.RecipientResponse;
import com.mailvault.mailbox.api.StorageUsageResponse;
import com.mailvault.mailbox.repository.AttachmentRow;
import com.mailvault.mailbox.repository.EmailHeaderRow;
import com.mailvault.mailbox.repository.EmailMessageRepository;
import com.mailvault.mailbox.repository.InboxRow;
import com.mailvault.mailbox.repository.RecipientRow;
import com.mailvault.mailbox.repository.StorageUsageRepository;
import com.mailvault.mailbox.repository.StorageUsageRow;
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
    private final StorageUsageRepository storageUsageRepository;

    public MailboxQueryService(
            EmailMessageRepository emailMessageRepository,
            StorageUsageRepository storageUsageRepository
    ) {
        this.emailMessageRepository = emailMessageRepository;
        this.storageUsageRepository = storageUsageRepository;
    }

    public List<InboxItemResponse> getInbox(String userId, int limit) {
        return emailMessageRepository.findInbox(userId, limit)
                .stream()
                .map(this::toInboxItem)
                .toList();
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

        return new EmailDetailResponse(
                email.id(),
                email.userId(),
                email.sender(),
                email.subject(),
                email.status(),
                email.receivedAt(),
                email.logicalSizeBytes(),
                recipients,
                attachments
        );
    }

    public StorageUsageResponse getStorageUsage(String userId) {
        StorageUsageRow usage = storageUsageRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "storage usage not found"));

        double usedPercent = usage.quotaBytes() == 0
                ? 0
                : (usage.usedBytes() * 100.0) / usage.quotaBytes();

        return new StorageUsageResponse(
                usage.userId(),
                usage.usedBytes(),
                usage.quotaBytes(),
                usedPercent,
                usage.updatedAt()
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
