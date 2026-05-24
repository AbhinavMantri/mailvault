package com.mailvault.quota.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@Configuration
@EnableConfigurationProperties(KafkaTopicProperties.class)
public class QuotaServiceConfiguration {
}
