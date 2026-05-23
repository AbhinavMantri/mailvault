package com.mailvault.ingestion.repository;

import com.mailvault.ingestion.domain.EmailMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EmailMessageRepository extends JpaRepository<EmailMessage, UUID> {
}

