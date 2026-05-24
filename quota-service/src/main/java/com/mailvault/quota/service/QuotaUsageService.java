package com.mailvault.quota.service;

import com.mailvault.quota.events.EmailReceivedEvent;
import com.mailvault.quota.repository.StorageUsageRepository;
import com.mailvault.quota.repository.StorageUsageRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class QuotaUsageService {

    private static final long DEFAULT_USER_QUOTA_BYTES = 5L * 1024 * 1024 * 1024;
    private static final String EMAIL_RECEIVED = "email.received";

    private final StorageUsageRepository storageUsageRepository;

    public QuotaUsageService(StorageUsageRepository storageUsageRepository) {
        this.storageUsageRepository = storageUsageRepository;
    }

    @Transactional
    public void recordEmailReceived(EmailReceivedEvent event) {
        Instant now = Instant.now();
        if (!storageUsageRepository.insertUsageEvent(
                event.eventId(),
                event.emailId(),
                event.userId(),
                event.logicalSizeBytes(),
                EMAIL_RECEIVED,
                now
        )) {
            return;
        }

        StorageUsageRow usage = storageUsageRepository.findByUserIdForUpdate(event.userId())
                .orElseGet(() -> storageUsageRepository.insertDefaultUsage(event.userId(), DEFAULT_USER_QUOTA_BYTES, now));
        storageUsageRepository.updateUsage(event.userId(), usage.usedBytes() + event.logicalSizeBytes(), now);
    }
}
