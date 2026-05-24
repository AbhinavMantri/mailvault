package com.mailvault.quota.api;

import com.mailvault.quota.service.QuotaQueryService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
public class QuotaController {

    private final QuotaQueryService quotaQueryService;

    public QuotaController(QuotaQueryService quotaQueryService) {
        this.quotaQueryService = quotaQueryService;
    }

    @GetMapping("/users/{userId}/storage")
    StorageUsageResponse storage(@PathVariable @NotBlank String userId) {
        return quotaQueryService.getStorageUsage(userId);
    }
}
