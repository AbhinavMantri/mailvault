package com.mailvault.searchindexer.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({KafkaTopicProperties.class, SearchIndexProperties.class})
public class SearchIndexerConfiguration {
}
