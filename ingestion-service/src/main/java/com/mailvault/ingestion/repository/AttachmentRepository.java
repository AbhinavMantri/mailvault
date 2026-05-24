package com.mailvault.ingestion.repository;

import com.mailvault.ingestion.domain.Attachment;
import com.mailvault.ingestion.domain.AttachmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {

    Optional<Attachment> findBySha256(String sha256);

    List<Attachment> findByIdInAndUserIdAndStatusIn(Collection<UUID> ids, String userId, Collection<AttachmentStatus> statuses);
}
