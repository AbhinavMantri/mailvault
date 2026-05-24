package com.mailvault.ingestion.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_threads")
public class UserThread {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String subjectNormalized;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ThreadFolder folder;

    @Column(nullable = false)
    private Instant lastMessageAt;

    @Column(nullable = false)
    private String lastSender;

    @Column(nullable = false)
    private int messageCount;

    @Column(nullable = false)
    private int unreadCount;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected UserThread() {
    }

    public UserThread(UUID id, String userId, String subjectNormalized, ThreadFolder folder,
                      Instant lastMessageAt, String lastSender, int messageCount, int unreadCount,
                      Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.subjectNormalized = subjectNormalized;
        this.folder = folder;
        this.lastMessageAt = lastMessageAt;
        this.lastSender = lastSender;
        this.messageCount = messageCount;
        this.unreadCount = unreadCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }
}
