package com.mailvault.ingestion.repository;

import com.mailvault.ingestion.domain.StorageUsage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StorageUsageRepository extends JpaRepository<StorageUsage, String> {
}

