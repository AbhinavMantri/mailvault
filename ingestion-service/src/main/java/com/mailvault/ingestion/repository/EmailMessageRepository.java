package com.mailvault.ingestion.repository;

import com.mailvault.ingestion.domain.EmailMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EmailMessageRepository extends JpaRepository<EmailMessage, UUID> {
    Optional<EmailMessage> findByIdAndUserId(UUID id, String userId);
}
