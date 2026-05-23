package com.mailvault.ingestion.events;

import com.mailvault.ingestion.config.KafkaTopicProperties;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class EmailEventPublisher {

    private final KafkaTemplate<String, EmailReceivedEvent> kafkaTemplate;
    private final KafkaTopicProperties topicProperties;

    public EmailEventPublisher(KafkaTemplate<String, EmailReceivedEvent> kafkaTemplate,
                               KafkaTopicProperties topicProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicProperties = topicProperties;
    }

    public void publishEmailReceived(EmailReceivedEvent event) {
        kafkaTemplate.send(topicProperties.emailReceivedTopic(), event.emailId().toString(), event);
    }
}

