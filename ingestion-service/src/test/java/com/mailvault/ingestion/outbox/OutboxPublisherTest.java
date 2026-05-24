package com.mailvault.ingestion.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mailvault.ingestion.events.AttachmentUploadedEvent;
import com.mailvault.ingestion.events.EmailEventPublisher;
import com.mailvault.ingestion.events.EmailReceivedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private EmailEventPublisher emailEventPublisher;

    @Test
    void publishBatchPublishesEmailReceivedAndMarksOutboxEventPublished() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        EmailReceivedEvent emailReceivedEvent = emailReceivedEvent();
        OutboxEvent outboxEvent = new OutboxEvent(
                emailReceivedEvent.eventId(),
                emailReceivedEvent.emailId(),
                OutboxEventService.EMAIL_RECEIVED,
                objectMapper.writeValueAsString(emailReceivedEvent),
                emailReceivedEvent.receivedAt()
        );
        when(outboxEventRepository.findUnpublished(100)).thenReturn(List.of(outboxEvent));
        when(emailEventPublisher.publishEmailReceived(any(EmailReceivedEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        new OutboxPublisher(objectMapper, outboxEventRepository, emailEventPublisher).publishBatch();

        verify(emailEventPublisher).publishEmailReceived(any(EmailReceivedEvent.class));
        verify(outboxEventRepository).markPublished(outboxEvent.id());
    }

    @Test
    void publishBatchPublishesAttachmentUploadedAndMarksOutboxEventPublished() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        AttachmentUploadedEvent attachmentUploadedEvent = attachmentUploadedEvent();
        OutboxEvent outboxEvent = new OutboxEvent(
                attachmentUploadedEvent.eventId(),
                attachmentUploadedEvent.attachmentId(),
                OutboxEventService.ATTACHMENT_UPLOADED,
                objectMapper.writeValueAsString(attachmentUploadedEvent),
                attachmentUploadedEvent.uploadedAt()
        );
        when(outboxEventRepository.findUnpublished(100)).thenReturn(List.of(outboxEvent));
        when(emailEventPublisher.publishAttachmentUploaded(any(AttachmentUploadedEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        new OutboxPublisher(objectMapper, outboxEventRepository, emailEventPublisher).publishBatch();

        verify(emailEventPublisher).publishAttachmentUploaded(any(AttachmentUploadedEvent.class));
        verify(outboxEventRepository).markPublished(outboxEvent.id());
    }

    @Test
    void publishBatchDoesNotMarkPublishedWhenKafkaPublishFails() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        EmailReceivedEvent emailReceivedEvent = emailReceivedEvent();
        OutboxEvent outboxEvent = new OutboxEvent(
                emailReceivedEvent.eventId(),
                emailReceivedEvent.emailId(),
                OutboxEventService.EMAIL_RECEIVED,
                objectMapper.writeValueAsString(emailReceivedEvent),
                emailReceivedEvent.receivedAt()
        );
        CompletableFuture<SendResult<String, Object>> kafkaFailure = new CompletableFuture<>();
        kafkaFailure.completeExceptionally(new RuntimeException("kafka unavailable"));
        when(outboxEventRepository.findUnpublished(100)).thenReturn(List.of(outboxEvent));
        when(emailEventPublisher.publishEmailReceived(any(EmailReceivedEvent.class))).thenReturn(kafkaFailure);

        assertThatThrownBy(() -> new OutboxPublisher(objectMapper, outboxEventRepository, emailEventPublisher).publishBatch())
                .isInstanceOf(RuntimeException.class);

        verify(outboxEventRepository, never()).markPublished(outboxEvent.id());
    }

    private EmailReceivedEvent emailReceivedEvent() {
        return new EmailReceivedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "user-123",
                "billing@example.com",
                List.of("abhinav@example.com"),
                "Invoice for May",
                512,
                Instant.parse("2026-05-23T12:00:00Z")
        );
    }

    private AttachmentUploadedEvent attachmentUploadedEvent() {
        UUID attachmentId = UUID.randomUUID();
        return new AttachmentUploadedEvent(
                UUID.randomUUID(),
                attachmentId,
                "user-123",
                "invoice.txt",
                "users/user-123/pending-attachments/%s/invoice.txt".formatted(attachmentId),
                "text/plain",
                512,
                Instant.parse("2026-05-23T12:00:00Z")
        );
    }
}
