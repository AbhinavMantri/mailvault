package com.mailvault.searchindexer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mailvault.search")
public record SearchIndexProperties(
        String baseUrl,
        String emailIndex
) {
}
