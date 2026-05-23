package com.mailvault.ingestion.repository;

import com.mailvault.ingestion.domain.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {

    Optional<Attachment> findBySha256(String sha256);
}

