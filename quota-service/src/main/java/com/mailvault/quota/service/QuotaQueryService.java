package com.mailvault.quota.service;

import com.mailvault.quota.api.StorageUsageResponse;
import com.mailvault.quota.repository.StorageUsageRepository;
import com.mailvault.quota.repository.StorageUsageRow;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class QuotaQueryService {

    private final StorageUsageRepository storageUsageRepository;

    public QuotaQueryService(StorageUsageRepository storageUsageRepository) {
        this.storageUsageRepository = storageUsageRepository;
    }

    public StorageUsageResponse getStorageUsage(String userId) {
        StorageUsageRow usage = storageUsageRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "storage usage not found"));

        double usedPercent = usage.quotaBytes() == 0
                ? 0
                : (usage.usedBytes() * 100.0) / usage.quotaBytes();

        return new StorageUsageResponse(
                usage.userId(),
                usage.usedBytes(),
                usage.quotaBytes(),
                usedPercent,
                usage.updatedAt()
        );
    }
}
