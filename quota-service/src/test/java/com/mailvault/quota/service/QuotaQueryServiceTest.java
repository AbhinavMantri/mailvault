package com.mailvault.quota.service;

import com.mailvault.quota.repository.StorageUsageRepository;
import com.mailvault.quota.repository.StorageUsageRow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuotaQueryServiceTest {

    @Mock
    private StorageUsageRepository storageUsageRepository;

    @InjectMocks
    private QuotaQueryService quotaQueryService;

    @Test
    void getStorageUsageCalculatesUsedPercentage() {
        Instant updatedAt = Instant.parse("2026-05-23T09:00:00Z");
        when(storageUsageRepository.findByUserId("user-123"))
                .thenReturn(Optional.of(new StorageUsageRow("user-123", 25L, 100L, updatedAt)));

        var response = quotaQueryService.getStorageUsage("user-123");

        assertThat(response.usedBytes()).isEqualTo(25L);
        assertThat(response.quotaBytes()).isEqualTo(100L);
        assertThat(response.usedPercent()).isEqualTo(25.0);
        assertThat(response.updatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void getStorageUsageReturnsNotFoundForMissingUser() {
        when(storageUsageRepository.findByUserId("user-123")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> quotaQueryService.getStorageUsage("user-123"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND");
    }
}
