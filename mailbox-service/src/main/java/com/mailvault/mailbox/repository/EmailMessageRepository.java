package com.mailvault.mailbox.repository;

import com.mailvault.mailbox.domain.EmailMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmailMessageRepository extends JpaRepository<EmailMessage, UUID> {

    List<EmailMessage> findByUserIdOrderByReceivedAtDesc(String userId, Pageable pageable);

    Optional<EmailMessage> findByIdAndUserId(UUID id, String userId);
}
