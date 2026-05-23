package com.mailvault.ingestion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mailvault.quota")
public record QuotaProperties(
        String baseUrl
) {
}
