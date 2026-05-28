package com.mailvault.ingestion.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "emails")
public class EmailMessage {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String sender;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false)
    private String rawObjectKey;

    @Column(nullable = false)
    private String textObjectKey;

    private String htmlObjectKey;

    @Column(nullable = false)
    private long logicalSizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmailStatus status;

    @Column(nullable = false)
    private Instant receivedAt;

    @OneToMany(mappedBy = "email", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<EmailRecipient> recipients = new ArrayList<>();

    @OneToMany(mappedBy = "email", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<EmailAttachmentRef> attachmentRefs = new ArrayList<>();

    protected EmailMessage() {
    }

    public EmailMessage(UUID id, String userId, String sender, String subject, String rawObjectKey,
                        String textObjectKey, String htmlObjectKey, long logicalSizeBytes,
                        EmailStatus status, Instant receivedAt) {
        this.id = id;
        this.userId = userId;
        this.sender = sender;
        this.subject = subject;
        this.rawObjectKey = rawObjectKey;
        this.textObjectKey = textObjectKey;
        this.htmlObjectKey = htmlObjectKey;
        this.logicalSizeBytes = logicalSizeBytes;
        this.status = status;
        this.receivedAt = receivedAt;
    }

    public UUID getId() {
        return id;
    }

    public long getLogicalSizeBytes() {
        return logicalSizeBytes;
    }

    public String getUserId() {
        return userId;
    }

    public String getSender() {
        return sender;
    }

    public String getSubject() {
        return subject;
    }

    public EmailStatus getStatus() {
        return status;
    }

    public List<EmailRecipient> getRecipients() {
        return List.copyOf(recipients);
    }

    public void submitDraft() {
        if (status != EmailStatus.DRAFT) {
            throw new IllegalArgumentException("Email is not a draft");
        }
        this.status = EmailStatus.INDEX_PENDING;
    }

    public void addRecipient(EmailRecipient recipient) {
        recipients.add(recipient);
        recipient.attachTo(this);
    }

    public void addAttachmentRef(EmailAttachmentRef attachmentRef) {
        attachmentRefs.add(attachmentRef);
        attachmentRef.attachTo(this);
    }
}
