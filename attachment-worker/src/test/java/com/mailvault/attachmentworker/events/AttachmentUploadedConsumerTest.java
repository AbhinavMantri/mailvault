package com.mailvault.attachmentworker.events;

import com.mailvault.attachmentworker.service.AttachmentProcessingService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AttachmentUploadedConsumerTest {

    @Test
    void onAttachmentUploadedDelegatesToProcessingService() {
        AttachmentProcessingService processingService = mock(AttachmentProcessingService.class);
        AttachmentUploadedConsumer consumer = new AttachmentUploadedConsumer(processingService);
        UUID attachmentId = UUID.randomUUID();

        consumer.onAttachmentUploaded(new AttachmentUploadedEvent(
                UUID.randomUUID(),
                attachmentId,
                "user-123",
                "invoice.txt",
                "pending-key",
                "text/plain",
                512,
                Instant.parse("2026-05-23T12:00:00Z")
        ));

        verify(processingService).process(attachmentId);
    }
}
