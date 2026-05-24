package com.mailvault.searchindexer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mailvault.idempotency")
public record IdempotencyProperties(
        String redisKeyPrefix,
        long ttlDays
) {
}
