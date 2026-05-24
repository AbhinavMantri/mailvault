package com.mailvault.ingestion.repository;

import com.mailvault.ingestion.domain.UserThread;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserThreadRepository extends JpaRepository<UserThread, UUID> {
}
