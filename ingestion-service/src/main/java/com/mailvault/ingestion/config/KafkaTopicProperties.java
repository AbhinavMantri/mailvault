package com.mailvault.ingestion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mailvault.kafka")
public record KafkaTopicProperties(String emailReceivedTopic) {
}

