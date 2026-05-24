package com.mailvault.attachmentworker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mailvault.storage")
public record StorageProperties(
        String endpoint,
        String accessKey,
        String secretKey,
        String bucket
) {
}
