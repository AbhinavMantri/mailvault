package com.mailvault.mailbox.service;

import com.mailvault.mailbox.api.AttachmentResponse;
import com.mailvault.mailbox.api.EmailDetailResponse;
import com.mailvault.mailbox.api.InboxItemResponse;
import com.mailvault.mailbox.api.RecipientResponse;
import com.mailvault.mailbox.api.StorageUsageResponse;
import com.mailvault.mailbox.domain.Attachment;
import com.mailvault.mailbox.domain.EmailAttachmentRef;
import com.mailvault.mailbox.domain.EmailMessage;
import com.mailvault.mailbox.domain.EmailRecipient;
import com.mailvault.mailbox.domain.StorageUsage;
import com.mailvault.mailbox.repository.EmailMessageRepository;
import com.mailvault.mailbox.repository.StorageUsageRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
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
        return emailMessageRepository.findByUserIdOrderByReceivedAtDesc(userId, PageRequest.of(0, limit))
                .stream()
                .map(this::toInboxItem)
                .toList();
    }

    public EmailDetailResponse getEmailDetail(String userId, UUID emailId) {
        EmailMessage email = emailMessageRepository.findByIdAndUserId(emailId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "email not found"));

        List<RecipientResponse> recipients = email.getRecipients().stream()
                .sorted(Comparator.comparing(EmailRecipient::getRecipientType)
                        .thenComparing(EmailRecipient::getRecipientAddress))
                .map(recipient -> new RecipientResponse(
                        recipient.getRecipientAddress(),
                        recipient.getRecipientType()
                ))
                .toList();

        List<AttachmentResponse> attachments = email.getAttachmentRefs().stream()
                .map(EmailAttachmentRef::getAttachment)
                .map(this::toAttachment)
                .toList();

        return new EmailDetailResponse(
                email.getId(),
                email.getUserId(),
                email.getSender(),
                email.getSubject(),
                email.getStatus(),
                email.getReceivedAt(),
                email.getLogicalSizeBytes(),
                recipients,
                attachments
        );
    }

    public StorageUsageResponse getStorageUsage(String userId) {
        StorageUsage usage = storageUsageRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "storage usage not found"));

        double usedPercent = usage.getQuotaBytes() == 0
                ? 0
                : (usage.getUsedBytes() * 100.0) / usage.getQuotaBytes();

        return new StorageUsageResponse(
                usage.getUserId(),
                usage.getUsedBytes(),
                usage.getQuotaBytes(),
                usedPercent,
                usage.getUpdatedAt()
        );
    }

    private InboxItemResponse toInboxItem(EmailMessage email) {
        return new InboxItemResponse(
                email.getId(),
                email.getSender(),
                email.getSubject(),
                email.getStatus(),
                email.getReceivedAt(),
                email.getLogicalSizeBytes(),
                email.getAttachmentRefs().size()
        );
    }

    private AttachmentResponse toAttachment(Attachment attachment) {
        return new AttachmentResponse(
                attachment.getId(),
                attachment.getFilename(),
                attachment.getContentType(),
                attachment.getSizeBytes(),
                attachment.getStatus()
        );
    }
}
