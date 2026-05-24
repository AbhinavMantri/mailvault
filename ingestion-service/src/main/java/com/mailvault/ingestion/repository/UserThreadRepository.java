package com.mailvault.ingestion.repository;

import com.mailvault.ingestion.domain.UserThread;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserThreadRepository extends JpaRepository<UserThread, UUID> {

    Optional<UserThread> findByIdAndUserId(UUID id, String userId);
}
