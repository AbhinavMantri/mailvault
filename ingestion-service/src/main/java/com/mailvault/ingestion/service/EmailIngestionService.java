package com.mailvault.ingestion.service;

import com.mailvault.ingestion.api.dto.AttachmentImportRequest;
import com.mailvault.ingestion.api.dto.EmailImportRequest;
import com.mailvault.ingestion.api.dto.EmailImportResponse;
import com.mailvault.ingestion.domain.Attachment;
import com.mailvault.ingestion.domain.EmailAttachmentRef;
import com.mailvault.ingestion.domain.EmailMessage;
import com.mailvault.ingestion.domain.EmailRecipient;
import com.mailvault.ingestion.domain.EmailStatus;
import com.mailvault.ingestion.domain.RecipientType;
import com.mailvault.ingestion.domain.StorageUsage;
import com.mailvault.ingestion.events.EmailEventPublisher;
import com.mailvault.ingestion.events.EmailReceivedEvent;
import com.mailvault.ingestion.repository.AttachmentRepository;
import com.mailvault.ingestion.repository.EmailMessageRepository;
import com.mailvault.ingestion.repository.StorageUsageRepository;
import com.mailvault.ingestion.storage.ObjectStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class EmailIngestionService {

    private static final long DEFAULT_USER_QUOTA_BYTES = 5L * 1024 * 1024 * 1024;

    private final ObjectStorageService objectStorageService;
    private final EmailMessageRepository emailMessageRepository;
    private final AttachmentRepository attachmentRepository;
    private final StorageUsageRepository storageUsageRepository;
    private final EmailEventPublisher emailEventPublisher;

    public EmailIngestionService(ObjectStorageService objectStorageService,
                                 EmailMessageRepository emailMessageRepository,
                                 AttachmentRepository attachmentRepository,
                                 StorageUsageRepository storageUsageRepository,
                                 EmailEventPublisher emailEventPublisher) {
        this.objectStorageService = objectStorageService;
        this.emailMessageRepository = emailMessageRepository;
        this.attachmentRepository = attachmentRepository;
        this.storageUsageRepository = storageUsageRepository;
        this.emailEventPublisher = emailEventPublisher;
    }

    @Transactional
    public EmailImportResponse importEmail(EmailImportRequest request) {
        UUID emailId = UUID.randomUUID();
        Instant receivedAt = Instant.now();
        List<DecodedAttachment> attachments = decodeAttachments(request.attachments());
        long logicalSizeBytes = calculateLogicalSize(request, attachments);

        StorageUsage storageUsage = storageUsageRepository.findById(request.userId())
                .orElseGet(() -> new StorageUsage(request.userId(), 0, DEFAULT_USER_QUOTA_BYTES, receivedAt));
        if (!storageUsage.canAccept(logicalSizeBytes)) {
            throw new IllegalArgumentException("User storage quota exceeded");
        }

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
        attachments.forEach(attachment -> email.addAttachmentRef(new EmailAttachmentRef(
                findOrCreateAttachment(attachment, request.userId(), receivedAt),
                attachment.filename()
        )));

        storageUsage.addUsage(logicalSizeBytes, receivedAt);
        storageUsageRepository.save(storageUsage);
        emailMessageRepository.save(email);

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

    private Attachment findOrCreateAttachment(DecodedAttachment decodedAttachment, String userId, Instant createdAt) {
        return attachmentRepository.findBySha256(decodedAttachment.sha256())
                .orElseGet(() -> {
                    UUID attachmentId = UUID.randomUUID();
                    String objectKey = "attachments/%s/%s".formatted(decodedAttachment.sha256(), decodedAttachment.filename());
                    objectStorageService.putBytes(objectKey, decodedAttachment.content(), decodedAttachment.contentType());
                    return attachmentRepository.save(new Attachment(
                            attachmentId,
                            decodedAttachment.sha256(),
                            objectKey,
                            decodedAttachment.contentType(),
                            decodedAttachment.content().length,
                            createdAt
                    ));
                });
    }

    private List<DecodedAttachment> decodeAttachments(List<AttachmentImportRequest> attachments) {
        if (attachments == null) {
            return List.of();
        }
        return attachments.stream()
                .map(this::decodeAttachment)
                .toList();
    }

    private DecodedAttachment decodeAttachment(AttachmentImportRequest request) {
        try {
            byte[] content = Base64.getDecoder().decode(request.base64Content());
            return new DecodedAttachment(
                    request.filename(),
                    request.contentType(),
                    content,
                    sha256(content)
            );
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Attachment " + request.filename() + " is not valid Base64", exception);
        }
    }

    private long calculateLogicalSize(EmailImportRequest request, List<DecodedAttachment> attachments) {
        long bodySize = safeText(request.textBody()).getBytes(StandardCharsets.UTF_8).length;
        long htmlSize = safeText(request.htmlBody()).getBytes(StandardCharsets.UTF_8).length;
        long attachmentSize = attachments.stream()
                .mapToLong(attachment -> attachment.content().length)
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

    private String sha256(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is not available", exception);
        }
    }

    private record DecodedAttachment(
            String filename,
            String contentType,
            byte[] content,
            String sha256
    ) {
    }
}

