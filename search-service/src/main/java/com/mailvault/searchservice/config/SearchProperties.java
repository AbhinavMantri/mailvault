package com.mailvault.searchservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mailvault.search")
public record SearchProperties(
        String baseUrl,
        String emailIndex
) {
}
