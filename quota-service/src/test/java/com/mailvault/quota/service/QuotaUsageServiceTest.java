package com.mailvault.quota.service;

import com.mailvault.quota.events.EmailReceivedEvent;
import com.mailvault.quota.repository.StorageUsageRepository;
import com.mailvault.quota.repository.StorageUsageRow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuotaUsageServiceTest {

    @Mock
    private StorageUsageRepository storageUsageRepository;

    @InjectMocks
    private QuotaUsageService quotaUsageService;

    @Test
    void recordEmailReceivedIncrementsUsageWhenEventIsNew() {
        EmailReceivedEvent event = emailReceivedEvent(512);
        Instant updatedAt = Instant.parse("2026-05-23T09:00:00Z");
        when(storageUsageRepository.insertUsageEvent(
                eq(event.eventId()),
                eq(event.emailId()),
                eq(event.userId()),
                eq(event.logicalSizeBytes()),
                eq("email.received"),
                any(Instant.class)
        )).thenReturn(true);
        when(storageUsageRepository.findByUserIdForUpdate("user-123"))
                .thenReturn(Optional.of(new StorageUsageRow("user-123", 100L, 1_000L, updatedAt)));

        quotaUsageService.recordEmailReceived(event);

        verify(storageUsageRepository).updateUsage(eq("user-123"), eq(612L), any(Instant.class));
    }

    @Test
    void recordEmailReceivedSkipsDuplicateEvent() {
        EmailReceivedEvent event = emailReceivedEvent(512);
        when(storageUsageRepository.insertUsageEvent(
                eq(event.eventId()),
                eq(event.emailId()),
                eq(event.userId()),
                eq(event.logicalSizeBytes()),
                eq("email.received"),
                any(Instant.class)
        )).thenReturn(false);

        quotaUsageService.recordEmailReceived(event);

        verify(storageUsageRepository, never()).findByUserIdForUpdate(any());
        verify(storageUsageRepository, never()).updateUsage(any(), eq(512L), any());
    }

    private EmailReceivedEvent emailReceivedEvent(long logicalSizeBytes) {
        return new EmailReceivedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "user-123",
                "billing@example.com",
                List.of("abhinav@example.com"),
                "Invoice for May",
                logicalSizeBytes,
                Instant.parse("2026-05-23T12:00:00Z")
        );
    }
}
