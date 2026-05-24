package com.mailvault.ingestion.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mailvault.ingestion.events.AttachmentUploadedEvent;
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
        switch (event.eventType()) {
            case OutboxEventService.EMAIL_RECEIVED -> emailEventPublisher
                    .publishEmailReceived(readEvent(event.payload(), EmailReceivedEvent.class))
                    .join();
            case OutboxEventService.ATTACHMENT_UPLOADED -> emailEventPublisher
                    .publishAttachmentUploaded(readEvent(event.payload(), AttachmentUploadedEvent.class))
                    .join();
            default -> throw new IllegalArgumentException("Unsupported outbox event type: " + event.eventType());
        }
        outboxEventRepository.markPublished(event.id());
    }

    private <T> T readEvent(String payload, Class<T> eventType) {
        try {
            return objectMapper.readValue(payload, eventType);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to deserialize outbox event", exception);
        }
    }
}
