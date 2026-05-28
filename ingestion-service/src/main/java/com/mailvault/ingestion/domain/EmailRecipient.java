package com.mailvault.ingestion.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "email_recipients")
public class EmailRecipient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "email_id", nullable = false)
    private EmailMessage email;

    @Column(nullable = false)
    private String recipientAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecipientType recipientType;

    private String recipientUserId;

    @Column(nullable = false)
    private String deliveryStatus;

    protected EmailRecipient() {
    }

    public EmailRecipient(String recipientAddress, RecipientType recipientType) {
        this.recipientAddress = recipientAddress;
        this.recipientType = recipientType;
        this.deliveryStatus = "DELIVERED";
    }

    public String getRecipientAddress() {
        return recipientAddress;
    }

    public RecipientType getRecipientType() {
        return recipientType;
    }

    void attachTo(EmailMessage email) {
        this.email = email;
    }
}
