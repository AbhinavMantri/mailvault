package com.mailvault.ingestion.service;

import com.mailvault.ingestion.api.dto.EmailDraftRequest;
import com.mailvault.ingestion.api.dto.EmailForwardRequest;
import com.mailvault.ingestion.api.dto.EmailImportRequest;
import com.mailvault.ingestion.api.dto.EmailImportResponse;
import com.mailvault.ingestion.api.dto.EmailReplyRequest;
import com.mailvault.ingestion.api.dto.SendDraftRequest;
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
import com.mailvault.ingestion.domain.MailboxThread;
import com.mailvault.ingestion.events.EmailReceivedEvent;
import com.mailvault.ingestion.outbox.OutboxEventService;
import com.mailvault.ingestion.repository.AttachmentRepository;
import com.mailvault.ingestion.repository.EmailMessageRepository;
import com.mailvault.ingestion.repository.ThreadMessageRepository;
import com.mailvault.ingestion.repository.MailboxThreadRepository;
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
    private final MailboxThreadRepository mailboxThreadRepository;
    private final ThreadMessageRepository threadMessageRepository;

    public EmailIngestionService(ObjectStorageService objectStorageService,
                                 EmailMessageRepository emailMessageRepository,
                                 AttachmentRepository attachmentRepository,
                                 OutboxEventService outboxEventService,
                                 MailboxThreadRepository mailboxThreadRepository,
                                 ThreadMessageRepository threadMessageRepository) {
        this.objectStorageService = objectStorageService;
        this.emailMessageRepository = emailMessageRepository;
        this.attachmentRepository = attachmentRepository;
        this.outboxEventService = outboxEventService;
        this.mailboxThreadRepository = mailboxThreadRepository;
        this.threadMessageRepository = threadMessageRepository;
    }

    @Transactional
    public EmailImportResponse importEmail(EmailImportRequest request) {
        MessageDraft draft = MessageDraft.from(request);
        UUID emailId = UUID.randomUUID();
        Instant receivedAt = Instant.now();
        PersistedEmail persistedEmail = persistMessage(draft, emailId, receivedAt, EmailStatus.INDEX_PENDING);
        MailboxThread thread = new MailboxThread(
                UUID.randomUUID(),
                draft.userId(),
                normalizeSubject(draft.subject()),
                ThreadFolder.INBOX,
                receivedAt,
                draft.from(),
                1,
                1,
                receivedAt,
                receivedAt
        );
        mailboxThreadRepository.save(thread);
        threadMessageRepository.save(new ThreadMessage(
                UUID.randomUUID(),
                thread,
                persistedEmail.email(),
                MessageDirection.INBOUND,
                null,
                receivedAt
        ));

        saveEmailReceivedEvent(draft, emailId, persistedEmail.logicalSizeBytes(), receivedAt);

        return new EmailImportResponse(emailId, "ACCEPTED", persistedEmail.logicalSizeBytes());
    }

    @Transactional
    public EmailImportResponse replyToThread(UUID threadId, EmailReplyRequest request) {
        MessageDraft draft = MessageDraft.from(request);
        MailboxThread thread = mailboxThreadRepository.findByIdAndUserId(threadId, request.userId())
                .orElseThrow(() -> new IllegalArgumentException("Thread not found"));

        UUID emailId = UUID.randomUUID();
        Instant sentAt = Instant.now();
        PersistedEmail persistedEmail = persistMessage(draft, emailId, sentAt, EmailStatus.INDEX_PENDING);
        thread.appendMessage(draft.from(), sentAt, MessageDirection.OUTBOUND);
        threadMessageRepository.save(new ThreadMessage(
                UUID.randomUUID(),
                thread,
                persistedEmail.email(),
                MessageDirection.OUTBOUND,
                sentAt,
                sentAt
        ));

        saveEmailReceivedEvent(draft, emailId, persistedEmail.logicalSizeBytes(), sentAt);

        return new EmailImportResponse(emailId, "ACCEPTED", persistedEmail.logicalSizeBytes());
    }

    @Transactional
    public EmailImportResponse createDraft(EmailDraftRequest request) {
        MessageDraft draft = MessageDraft.from(request);
        UUID emailId = UUID.randomUUID();
        Instant createdAt = Instant.now();
        PersistedEmail persistedEmail = persistMessage(draft, emailId, createdAt, EmailStatus.DRAFT);
        MailboxThread thread = new MailboxThread(
                UUID.randomUUID(),
                draft.userId(),
                normalizeSubject(draft.subject()),
                ThreadFolder.DRAFT,
                createdAt,
                draft.from(),
                1,
                0,
                createdAt,
                createdAt
        );
        mailboxThreadRepository.save(thread);
        threadMessageRepository.save(new ThreadMessage(
                UUID.randomUUID(),
                thread,
                persistedEmail.email(),
                MessageDirection.DRAFT,
                createdAt,
                createdAt
        ));

        return new EmailImportResponse(emailId, "DRAFT", persistedEmail.logicalSizeBytes());
    }

    @Transactional
    public EmailImportResponse sendDraft(UUID emailId, SendDraftRequest request) {
        ThreadMessage threadMessage = threadMessageRepository.findByEmailIdAndEmailUserId(emailId, request.userId())
                .orElseThrow(() -> new IllegalArgumentException("Draft not found"));
        EmailMessage email = threadMessage.getEmail();
        email.submitDraft();

        Instant sentAt = Instant.now();
        threadMessage.sendDraft(sentAt);
        threadMessage.getThread().activateFromDraft(email.getSender(), sentAt);

        saveEmailReceivedEvent(email, sentAt);

        return new EmailImportResponse(emailId, "ACCEPTED", email.getLogicalSizeBytes());
    }

    @Transactional
    public EmailImportResponse forwardEmail(UUID originalEmailId, EmailForwardRequest request) {
        EmailMessage originalEmail = emailMessageRepository.findByIdAndUserId(originalEmailId, request.userId())
                .orElseThrow(() -> new IllegalArgumentException("Email not found"));
        MessageDraft draft = MessageDraft.from(
                request,
                forwardSubject(request.subject(), originalEmail.getSubject()),
                forwardTextBody(request.textBody(), originalEmail),
                forwardHtmlBody(request.htmlBody(), originalEmail)
        );
        List<Attachment> originalAttachments = request.includeOriginalAttachments()
                ? originalEmail.getAttachmentRefs().stream()
                        .map(EmailAttachmentRef::getAttachment)
                        .toList()
                : List.of();

        UUID emailId = UUID.randomUUID();
        Instant sentAt = Instant.now();
        PersistedEmail persistedEmail = persistMessage(
                draft,
                emailId,
                sentAt,
                EmailStatus.INDEX_PENDING,
                combinedForwardAttachments(draft, originalAttachments)
        );
        MailboxThread thread = new MailboxThread(
                UUID.randomUUID(),
                draft.userId(),
                normalizeSubject(draft.subject()),
                ThreadFolder.ACTIVE,
                sentAt,
                draft.from(),
                1,
                0,
                sentAt,
                sentAt
        );
        mailboxThreadRepository.save(thread);
        threadMessageRepository.save(new ThreadMessage(
                UUID.randomUUID(),
                thread,
                persistedEmail.email(),
                MessageDirection.OUTBOUND,
                sentAt,
                sentAt
        ));

        saveEmailReceivedEvent(draft, emailId, persistedEmail.logicalSizeBytes(), sentAt);

        return new EmailImportResponse(emailId, "ACCEPTED", persistedEmail.logicalSizeBytes());
    }

    private PersistedEmail persistMessage(MessageDraft draft, UUID emailId, Instant receivedAt, EmailStatus status) {
        List<Attachment> attachments = findUploadedAttachments(draft);
        return persistMessage(draft, emailId, receivedAt, status, attachments);
    }

    private PersistedEmail persistMessage(MessageDraft draft, UUID emailId, Instant receivedAt, EmailStatus status,
                                          List<Attachment> attachments) {
        long logicalSizeBytes = calculateLogicalSize(draft, attachments);

        String rawObjectKey = "users/%s/emails/%s/raw.eml".formatted(draft.userId(), emailId);
        String textObjectKey = "users/%s/emails/%s/body.txt".formatted(draft.userId(), emailId);
        String htmlObjectKey = draft.htmlBody() == null || draft.htmlBody().isBlank()
                ? null
                : "users/%s/emails/%s/body.html".formatted(draft.userId(), emailId);

        objectStorageService.putText(rawObjectKey, buildRawMime(draft), "message/rfc822");
        objectStorageService.putText(textObjectKey, safeText(draft.textBody()), "text/plain");
        if (htmlObjectKey != null) {
            objectStorageService.putText(htmlObjectKey, draft.htmlBody(), "text/html");
        }

        EmailMessage email = new EmailMessage(
                emailId,
                draft.userId(),
                draft.from(),
                subjectOrDefault(draft.subject()),
                rawObjectKey,
                textObjectKey,
                htmlObjectKey,
                logicalSizeBytes,
                status,
                receivedAt
        );
        safeList(draft.to()).forEach(recipient -> email.addRecipient(new EmailRecipient(recipient, RecipientType.TO)));
        safeList(draft.cc()).forEach(recipient -> email.addRecipient(new EmailRecipient(recipient, RecipientType.CC)));
        safeList(draft.bcc()).forEach(recipient -> email.addRecipient(new EmailRecipient(recipient, RecipientType.BCC)));
        attachments.forEach(attachment -> email.addAttachmentRef(new EmailAttachmentRef(attachment)));

        emailMessageRepository.save(email);
        return new PersistedEmail(email, logicalSizeBytes);
    }

    private List<Attachment> findUploadedAttachments(MessageDraft draft) {
        if (draft.attachmentIds() == null || draft.attachmentIds().isEmpty()) {
            return List.of();
        }
        List<Attachment> attachments = attachmentRepository.findByIdInAndUserIdAndStatusIn(
                draft.attachmentIds(),
                draft.userId(),
                IMPORTABLE_ATTACHMENT_STATUSES
        );
        if (attachments.size() != draft.attachmentIds().size()) {
            throw new IllegalArgumentException("All attachments must be uploaded before email import");
        }
        return attachments;
    }

    private long calculateLogicalSize(MessageDraft draft, List<Attachment> attachments) {
        long bodySize = safeText(draft.textBody()).getBytes(StandardCharsets.UTF_8).length;
        long htmlSize = safeText(draft.htmlBody()).getBytes(StandardCharsets.UTF_8).length;
        long attachmentSize = attachments.stream()
                .mapToLong(Attachment::getSizeBytes)
                .sum();
        return bodySize + htmlSize + attachmentSize;
    }

    private String buildRawMime(MessageDraft draft) {
        return """
                From: %s
                To: %s
                Cc: %s
                Subject: %s
                Content-Type: text/plain; charset=UTF-8

                %s
                """.formatted(
                draft.from(),
                String.join(",", safeList(draft.to())),
                String.join(",", safeList(draft.cc())),
                subjectOrDefault(draft.subject()),
                safeText(draft.textBody())
        );
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private List<String> safeList(List<String> value) {
        return value == null ? List.of() : value;
    }

    private String subjectOrDefault(String subject) {
        return subject == null || subject.isBlank() ? "(no subject)" : subject;
    }

    private String forwardSubject(String requestedSubject, String originalSubject) {
        if (requestedSubject != null && !requestedSubject.isBlank()) {
            return requestedSubject;
        }
        String subject = subjectOrDefault(originalSubject);
        String normalized = subject.trim().toLowerCase();
        if (normalized.startsWith("fw:") || normalized.startsWith("fwd:")) {
            return subject;
        }
        return "Fwd: " + subject;
    }

    private String forwardTextBody(String requestedBody, EmailMessage originalEmail) {
        String originalBody = objectStorageService.readText(originalEmail.getTextObjectKey());
        return """
                %s

                ---------- Forwarded message ---------
                From: %s
                Subject: %s

                %s
                """.formatted(
                safeText(requestedBody).trim(),
                originalEmail.getSender(),
                originalEmail.getSubject(),
                safeText(originalBody).trim()
        ).strip();
    }

    private String forwardHtmlBody(String requestedHtml, EmailMessage originalEmail) {
        String originalHtml = safeText(objectStorageService.readText(originalEmail.getHtmlObjectKey()));
        if (requestedHtml == null && originalHtml.isBlank()) {
            return null;
        }
        return """
                %s
                <hr>
                <p><strong>Forwarded message</strong></p>
                <p><strong>From:</strong> %s<br><strong>Subject:</strong> %s</p>
                %s
                """.formatted(
                safeText(requestedHtml),
                escapeHtml(originalEmail.getSender()),
                escapeHtml(originalEmail.getSubject()),
                originalHtml.isBlank() ? "<pre>%s</pre>".formatted(escapeHtml(objectStorageService.readText(originalEmail.getTextObjectKey()))) : originalHtml
        ).strip();
    }

    private List<Attachment> combinedForwardAttachments(MessageDraft draft, List<Attachment> originalAttachments) {
        List<Attachment> uploadedAttachments = findUploadedAttachments(draft);
        return java.util.stream.Stream.concat(originalAttachments.stream(), uploadedAttachments.stream())
                .distinct()
                .toList();
    }

    private String escapeHtml(String value) {
        return safeText(value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private List<String> visibleRecipients(MessageDraft draft) {
        return java.util.stream.Stream.concat(safeList(draft.to()).stream(), safeList(draft.cc()).stream())
                .toList();
    }

    private void saveEmailReceivedEvent(MessageDraft draft, UUID emailId, long logicalSizeBytes, Instant receivedAt) {
        outboxEventService.saveEmailReceived(new EmailReceivedEvent(
                UUID.randomUUID(),
                emailId,
                draft.userId(),
                draft.from(),
                visibleRecipients(draft),
                draft.subject(),
                logicalSizeBytes,
                receivedAt
        ), receivedAt);
    }

    private void saveEmailReceivedEvent(EmailMessage email, Instant receivedAt) {
        outboxEventService.saveEmailReceived(new EmailReceivedEvent(
                UUID.randomUUID(),
                email.getId(),
                email.getUserId(),
                email.getSender(),
                visibleRecipients(email),
                email.getSubject(),
                email.getLogicalSizeBytes(),
                receivedAt
        ), receivedAt);
    }

    private List<String> visibleRecipients(EmailMessage email) {
        return email.getRecipients().stream()
                .filter(recipient -> recipient.getRecipientType() != RecipientType.BCC)
                .map(EmailRecipient::getRecipientAddress)
                .toList();
    }

    private String normalizeSubject(String subject) {
        String normalized = subjectOrDefault(subject).trim().toLowerCase();
        while (normalized.startsWith("re:") || normalized.startsWith("fw:") || normalized.startsWith("fwd:")) {
            normalized = normalized.substring(normalized.indexOf(':') + 1).trim();
        }
        return normalized;
    }

    private record PersistedEmail(EmailMessage email, long logicalSizeBytes) {
    }

    private record MessageDraft(
            String userId,
            String from,
            List<String> to,
            List<String> cc,
            List<String> bcc,
            String subject,
            String textBody,
            String htmlBody,
            List<UUID> attachmentIds
    ) {
        static MessageDraft from(EmailImportRequest request) {
            return new MessageDraft(
                    request.userId(),
                    request.from(),
                    request.to(),
                    request.cc(),
                    request.bcc(),
                    request.subject(),
                    request.textBody(),
                    request.htmlBody(),
                    request.attachmentIds()
            );
        }

        static MessageDraft from(EmailReplyRequest request) {
            return new MessageDraft(
                    request.userId(),
                    request.from(),
                    request.to(),
                    request.cc(),
                    request.bcc(),
                    request.subject(),
                    request.textBody(),
                    request.htmlBody(),
                    request.attachmentIds()
            );
        }

        static MessageDraft from(EmailDraftRequest request) {
            return new MessageDraft(
                    request.userId(),
                    request.from(),
                    request.to(),
                    request.cc(),
                    request.bcc(),
                    request.subject(),
                    request.textBody(),
                    request.htmlBody(),
                    request.attachmentIds()
            );
        }

        static MessageDraft from(EmailForwardRequest request, String subject, String textBody, String htmlBody) {
            return new MessageDraft(
                    request.userId(),
                    request.from(),
                    request.to(),
                    request.cc(),
                    request.bcc(),
                    subject,
                    textBody,
                    htmlBody,
                    request.attachmentIds()
            );
        }
    }

}
