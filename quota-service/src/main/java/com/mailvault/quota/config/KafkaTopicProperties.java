package com.mailvault.quota.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mailvault.kafka")
public record KafkaTopicProperties(
        String emailReceivedTopic,
        String consumerGroupId
) {
}
