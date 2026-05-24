package com.mailvault.ingestion.repository;

import com.mailvault.ingestion.domain.MailboxThread;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MailboxThreadRepository extends JpaRepository<MailboxThread, UUID> {

    Optional<MailboxThread> findByIdAndUserId(UUID id, String userId);
}
