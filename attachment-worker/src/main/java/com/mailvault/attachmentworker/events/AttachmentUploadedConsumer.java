package com.mailvault.attachmentworker.events;

import com.mailvault.attachmentworker.service.AttachmentProcessingService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AttachmentUploadedConsumer {

    private final AttachmentProcessingService attachmentProcessingService;

    public AttachmentUploadedConsumer(AttachmentProcessingService attachmentProcessingService) {
        this.attachmentProcessingService = attachmentProcessingService;
    }

    @KafkaListener(
            topics = "${mailvault.kafka.attachment-uploaded-topic}",
            groupId = "${mailvault.kafka.consumer-group-id}"
    )
    public void onAttachmentUploaded(AttachmentUploadedEvent event) {
        attachmentProcessingService.process(event.attachmentId());
    }
}
