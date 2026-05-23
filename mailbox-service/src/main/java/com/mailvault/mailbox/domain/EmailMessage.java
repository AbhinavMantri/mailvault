package com.mailvault.mailbox.domain;

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

    public UUID getId() {
        return id;
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

    public String getRawObjectKey() {
        return rawObjectKey;
    }

    public String getTextObjectKey() {
        return textObjectKey;
    }

    public String getHtmlObjectKey() {
        return htmlObjectKey;
    }

    public long getLogicalSizeBytes() {
        return logicalSizeBytes;
    }

    public EmailStatus getStatus() {
        return status;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public List<EmailRecipient> getRecipients() {
        return recipients;
    }

    public List<EmailAttachmentRef> getAttachmentRefs() {
        return attachmentRefs;
    }
}

