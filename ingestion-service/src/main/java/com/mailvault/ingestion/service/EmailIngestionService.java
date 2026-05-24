package com.mailvault.ingestion.service;

import com.mailvault.ingestion.api.dto.EmailImportRequest;
import com.mailvault.ingestion.api.dto.EmailImportResponse;
import com.mailvault.ingestion.domain.Attachment;
import com.mailvault.ingestion.domain.AttachmentStatus;
import com.mailvault.ingestion.domain.EmailAttachmentRef;
import com.mailvault.ingestion.domain.EmailMessage;
import com.mailvault.ingestion.domain.EmailRecipient;
import com.mailvault.ingestion.domain.EmailStatus;
import com.mailvault.ingestion.domain.MessageDirection;
import com.mailvault.ingestion.domain.RecipientType;
import com.mailvault.ingestion.domain.ThreadFolder;
import com.mailvault.ingestion.domain.ThreadMessage;
import com.mailvault.ingestion.domain.UserThread;
import com.mailvault.ingestion.events.EmailReceivedEvent;
import com.mailvault.ingestion.outbox.OutboxEventService;
import com.mailvault.ingestion.repository.AttachmentRepository;
import com.mailvault.ingestion.repository.EmailMessageRepository;
import com.mailvault.ingestion.repository.ThreadMessageRepository;
import com.mailvault.ingestion.repository.UserThreadRepository;
import com.mailvault.ingestion.storage.ObjectStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class EmailIngestionService {

    private static final List<AttachmentStatus> IMPORTABLE_ATTACHMENT_STATUSES = List.of(
            AttachmentStatus.UPLOADED,
            AttachmentStatus.PROCESSING,
            AttachmentStatus.READY
    );

    private final ObjectStorageService objectStorageService;
    private final EmailMessageRepository emailMessageRepository;
    private final AttachmentRepository attachmentRepository;
    private final OutboxEventService outboxEventService;
    private final UserThreadRepository userThreadRepository;
    private final ThreadMessageRepository threadMessageRepository;

    public EmailIngestionService(ObjectStorageService objectStorageService,
                                 EmailMessageRepository emailMessageRepository,
                                 AttachmentRepository attachmentRepository,
                                 OutboxEventService outboxEventService,
                                 UserThreadRepository userThreadRepository,
                                 ThreadMessageRepository threadMessageRepository) {
        this.objectStorageService = objectStorageService;
        this.emailMessageRepository = emailMessageRepository;
        this.attachmentRepository = attachmentRepository;
        this.outboxEventService = outboxEventService;
        this.userThreadRepository = userThreadRepository;
        this.threadMessageRepository = threadMessageRepository;
    }

    @Transactional
    public EmailImportResponse importEmail(EmailImportRequest request) {
        UUID emailId = UUID.randomUUID();
        Instant receivedAt = Instant.now();
        List<Attachment> attachments = findUploadedAttachments(request);
        long logicalSizeBytes = calculateLogicalSize(request, attachments);

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
        safeList(request.to()).forEach(recipient -> email.addRecipient(new EmailRecipient(recipient, RecipientType.TO)));
        safeList(request.cc()).forEach(recipient -> email.addRecipient(new EmailRecipient(recipient, RecipientType.CC)));
        safeList(request.bcc()).forEach(recipient -> email.addRecipient(new EmailRecipient(recipient, RecipientType.BCC)));
        attachments.forEach(attachment -> email.addAttachmentRef(new EmailAttachmentRef(attachment)));

        emailMessageRepository.save(email);
        UserThread thread = new UserThread(
                UUID.randomUUID(),
                request.userId(),
                normalizeSubject(request.subject()),
                ThreadFolder.INBOX,
                receivedAt,
                request.from(),
                1,
                1,
                receivedAt,
                receivedAt
        );
        userThreadRepository.save(thread);
        threadMessageRepository.save(new ThreadMessage(
                UUID.randomUUID(),
                thread,
                email,
                MessageDirection.INBOUND,
                null,
                receivedAt
        ));

        outboxEventService.saveEmailReceived(new EmailReceivedEvent(
                UUID.randomUUID(),
                emailId,
                request.userId(),
                request.from(),
                visibleRecipients(request),
                request.subject(),
                logicalSizeBytes,
                receivedAt
        ), receivedAt);

        return new EmailImportResponse(emailId, "ACCEPTED", logicalSizeBytes);
    }

    private List<Attachment> findUploadedAttachments(EmailImportRequest request) {
        if (request.attachmentIds() == null || request.attachmentIds().isEmpty()) {
            return List.of();
        }
        List<Attachment> attachments = attachmentRepository.findByIdInAndUserIdAndStatusIn(
                request.attachmentIds(),
                request.userId(),
                IMPORTABLE_ATTACHMENT_STATUSES
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
                Cc: %s
                Subject: %s
                Content-Type: text/plain; charset=UTF-8

                %s
                """.formatted(
                request.from(),
                String.join(",", safeList(request.to())),
                String.join(",", safeList(request.cc())),
                request.subject(),
                safeText(request.textBody())
        );
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private List<String> safeList(List<String> value) {
        return value == null ? List.of() : value;
    }

    private List<String> visibleRecipients(EmailImportRequest request) {
        return java.util.stream.Stream.concat(safeList(request.to()).stream(), safeList(request.cc()).stream())
                .toList();
    }

    private String normalizeSubject(String subject) {
        String normalized = safeText(subject).trim().toLowerCase();
        while (normalized.startsWith("re:") || normalized.startsWith("fw:") || normalized.startsWith("fwd:")) {
            normalized = normalized.substring(normalized.indexOf(':') + 1).trim();
        }
        return normalized;
    }

}
