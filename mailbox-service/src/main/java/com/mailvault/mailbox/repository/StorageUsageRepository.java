package com.mailvault.mailbox.repository;

import com.mailvault.mailbox.domain.StorageUsage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StorageUsageRepository extends JpaRepository<StorageUsage, String> {
}

