package com.mailvault.ingestion.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "thread_messages")
public class ThreadMessage {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id", nullable = false)
    private MailboxThread thread;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "email_id", nullable = false)
    private EmailMessage email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageDirection direction;

    private Instant readAt;

    @Column(nullable = false)
    private Instant createdAt;

    protected ThreadMessage() {
    }

    public ThreadMessage(UUID id, MailboxThread thread, EmailMessage email, MessageDirection direction,
                         Instant readAt, Instant createdAt) {
        this.id = id;
        this.thread = thread;
        this.email = email;
        this.direction = direction;
        this.readAt = readAt;
        this.createdAt = createdAt;
    }

    public MailboxThread getThread() {
        return thread;
    }

    public EmailMessage getEmail() {
        return email;
    }

    public MessageDirection getDirection() {
        return direction;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void sendDraft(Instant sentAt) {
        if (direction != MessageDirection.DRAFT) {
            throw new IllegalArgumentException("Thread message is not a draft");
        }
        this.direction = MessageDirection.OUTBOUND;
        this.readAt = sentAt;
        this.createdAt = sentAt;
    }
}
