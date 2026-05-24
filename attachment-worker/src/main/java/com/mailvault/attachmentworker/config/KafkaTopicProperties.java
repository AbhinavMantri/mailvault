package com.mailvault.attachmentworker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mailvault.kafka")
public record KafkaTopicProperties(
        String attachmentUploadedTopic,
        String consumerGroupId
) {
}
