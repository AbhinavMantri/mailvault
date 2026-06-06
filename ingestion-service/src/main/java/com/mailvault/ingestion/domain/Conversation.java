package com.mailvault.ingestion.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "conversations")
public class Conversation {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String subjectNormalized;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Conversation() {
    }

    public Conversation(UUID id, String subjectNormalized, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.subjectNormalized = subjectNormalized;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getSubjectNormalized() {
        return subjectNormalized;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void touch(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

}
