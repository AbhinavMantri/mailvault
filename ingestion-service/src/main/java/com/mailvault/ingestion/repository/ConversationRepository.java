package com.mailvault.ingestion.repository;

import com.mailvault.ingestion.domain.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
}
