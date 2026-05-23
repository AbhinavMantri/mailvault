package com.mailvault.ingestion.service;

import com.mailvault.ingestion.api.dto.EmailImportRequest;
import com.mailvault.ingestion.api.dto.EmailImportResponse;
import com.mailvault.ingestion.domain.Attachment;
import com.mailvault.ingestion.domain.AttachmentStatus;
import com.mailvault.ingestion.domain.EmailAttachmentRef;
import com.mailvault.ingestion.domain.EmailMessage;
import com.mailvault.ingestion.domain.EmailRecipient;
import com.mailvault.ingestion.domain.EmailStatus;
import com.mailvault.ingestion.domain.RecipientType;
import com.mailvault.ingestion.events.EmailEventPublisher;
import com.mailvault.ingestion.events.EmailReceivedEvent;
import com.mailvault.ingestion.quota.QuotaClient;
import com.mailvault.ingestion.repository.AttachmentRepository;
import com.mailvault.ingestion.repository.EmailMessageRepository;
import com.mailvault.ingestion.storage.ObjectStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class EmailIngestionService {

    private final ObjectStorageService objectStorageService;
    private final EmailMessageRepository emailMessageRepository;
    private final AttachmentRepository attachmentRepository;
    private final QuotaClient quotaClient;
    private final EmailEventPublisher emailEventPublisher;

    public EmailIngestionService(ObjectStorageService objectStorageService,
                                 EmailMessageRepository emailMessageRepository,
                                 AttachmentRepository attachmentRepository,
                                 QuotaClient quotaClient,
                                 EmailEventPublisher emailEventPublisher) {
        this.objectStorageService = objectStorageService;
        this.emailMessageRepository = emailMessageRepository;
        this.attachmentRepository = attachmentRepository;
        this.quotaClient = quotaClient;
        this.emailEventPublisher = emailEventPublisher;
    }

    @Transactional
    public EmailImportResponse importEmail(EmailImportRequest request) {
        UUID emailId = UUID.randomUUID();
        Instant receivedAt = Instant.now();
        List<Attachment> attachments = findUploadedAttachments(request);
        long logicalSizeBytes = calculateLogicalSize(request, attachments);

        quotaClient.reserve(request.userId(), logicalSizeBytes);

        String rawObjectKey = "users/%s/emails/%s/raw.eml".formatted(request.userId(), emailId);
        String textObjectKey = "users/%s/emails/%s/body.txt".formatted(request.userId(), emailId);
        String htmlObjectKey = request.htmlBody() == null || request.htmlBody().isBlank()
                ? null
                : "users/%s/emails/%s/body.html".formatted(request.userId(), emailId);

        objectStorageService.putText(rawObjectKey, buildRawMime(request), "message/rfc822");
        objectStorageService.putText(textObjectKey, safeText(request.textBody()), "text/plain");
        if (htmlObjectKey != null) {
            objectStorageService.putText(htmlObjectKey, request.htmlBody(), "text/html");
        }

        EmailMessage email = new EmailMessage(
                emailId,
                request.userId(),
                request.from(),
                request.subject(),
                rawObjectKey,
                textObjectKey,
                htmlObjectKey,
                logicalSizeBytes,
                EmailStatus.INDEX_PENDING,
                receivedAt
        );
        request.to().forEach(recipient -> email.addRecipient(new EmailRecipient(recipient, RecipientType.TO)));
        attachments.forEach(attachment -> email.addAttachmentRef(new EmailAttachmentRef(attachment)));

        emailMessageRepository.save(email);

        // TODO: replace direct Kafka publish with transactional outbox and idempotency key support.
        emailEventPublisher.publishEmailReceived(new EmailReceivedEvent(
                UUID.randomUUID(),
                emailId,
                request.userId(),
                request.from(),
                request.to(),
                request.subject(),
                logicalSizeBytes,
                receivedAt
        ));

        return new EmailImportResponse(emailId, "ACCEPTED", logicalSizeBytes);
    }

    private List<Attachment> findUploadedAttachments(EmailImportRequest request) {
        if (request.attachmentIds() == null || request.attachmentIds().isEmpty()) {
            return List.of();
        }
        List<Attachment> attachments = attachmentRepository.findByIdInAndUserIdAndStatus(
                request.attachmentIds(),
                request.userId(),
                AttachmentStatus.UPLOADED
        );
        if (attachments.size() != request.attachmentIds().size()) {
            throw new IllegalArgumentException("All attachments must be uploaded before email import");
        }
        return attachments;
    }

    private long calculateLogicalSize(EmailImportRequest request, List<Attachment> attachments) {
        long bodySize = safeText(request.textBody()).getBytes(StandardCharsets.UTF_8).length;
        long htmlSize = safeText(request.htmlBody()).getBytes(StandardCharsets.UTF_8).length;
        long attachmentSize = attachments.stream()
                .mapToLong(Attachment::getSizeBytes)
                .sum();
        return bodySize + htmlSize + attachmentSize;
    }

    private String buildRawMime(EmailImportRequest request) {
        return """
                From: %s
                To: %s
                Subject: %s
                Content-Type: text/plain; charset=UTF-8

                %s
                """.formatted(request.from(), String.join(",", request.to()), request.subject(), safeText(request.textBody()));
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

}
