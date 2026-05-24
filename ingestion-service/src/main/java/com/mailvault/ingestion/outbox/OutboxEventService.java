package com.mailvault.ingestion.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mailvault.ingestion.events.EmailReceivedEvent;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class OutboxEventService {

    public static final String EMAIL_RECEIVED = "email.received";

    private final ObjectMapper objectMapper;
    private final OutboxEventRepository outboxEventRepository;

    public OutboxEventService(ObjectMapper objectMapper, OutboxEventRepository outboxEventRepository) {
        this.objectMapper = objectMapper;
        this.outboxEventRepository = outboxEventRepository;
    }

    public void saveEmailReceived(EmailReceivedEvent event, Instant createdAt) {
        outboxEventRepository.save(new OutboxEvent(
                event.eventId(),
                event.emailId(),
                EMAIL_RECEIVED,
                toJson(event),
                createdAt
        ));
    }

    private String toJson(EmailReceivedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize outbox event", exception);
        }
    }
}
