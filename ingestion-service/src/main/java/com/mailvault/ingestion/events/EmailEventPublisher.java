package com.mailvault.ingestion.events;

import com.mailvault.ingestion.config.KafkaTopicProperties;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class EmailEventPublisher {

    private final KafkaTemplate<String, EmailReceivedEvent> kafkaTemplate;
    private final KafkaTopicProperties topicProperties;

    public EmailEventPublisher(KafkaTemplate<String, EmailReceivedEvent> kafkaTemplate,
                               KafkaTopicProperties topicProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicProperties = topicProperties;
    }

    public CompletableFuture<SendResult<String, EmailReceivedEvent>> publishEmailReceived(EmailReceivedEvent event) {
        return kafkaTemplate.send(topicProperties.emailReceivedTopic(), event.emailId().toString(), event);
    }
}
