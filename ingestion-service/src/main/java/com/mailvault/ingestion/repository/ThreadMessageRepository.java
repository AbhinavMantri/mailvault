package com.mailvault.ingestion.repository;

import com.mailvault.ingestion.domain.ThreadMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ThreadMessageRepository extends JpaRepository<ThreadMessage, UUID> {
}
