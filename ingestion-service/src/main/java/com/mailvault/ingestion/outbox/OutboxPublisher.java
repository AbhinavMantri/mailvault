package com.mailvault.ingestion.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mailvault.ingestion.events.EmailEventPublisher;
import com.mailvault.ingestion.events.EmailReceivedEvent;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxPublisher {

    private static final int BATCH_SIZE = 100;

    private final ObjectMapper objectMapper;
    private final OutboxEventRepository outboxEventRepository;
    private final EmailEventPublisher emailEventPublisher;

    public OutboxPublisher(
            ObjectMapper objectMapper,
            OutboxEventRepository outboxEventRepository,
            EmailEventPublisher emailEventPublisher
    ) {
        this.objectMapper = objectMapper;
        this.outboxEventRepository = outboxEventRepository;
        this.emailEventPublisher = emailEventPublisher;
    }

    @Scheduled(fixedDelayString = "${mailvault.outbox.publish-delay-ms:1000}")
    public void publishBatch() {
        outboxEventRepository.findUnpublished(BATCH_SIZE).forEach(this::publish);
    }

    private void publish(OutboxEvent event) {
        if (!OutboxEventService.EMAIL_RECEIVED.equals(event.eventType())) {
            throw new IllegalArgumentException("Unsupported outbox event type: " + event.eventType());
        }
        EmailReceivedEvent emailReceivedEvent = readEmailReceived(event.payload());
        emailEventPublisher.publishEmailReceived(emailReceivedEvent).join();
        outboxEventRepository.markPublished(event.id());
    }

    private EmailReceivedEvent readEmailReceived(String payload) {
        try {
            return objectMapper.readValue(payload, EmailReceivedEvent.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to deserialize outbox event", exception);
        }
    }
}
