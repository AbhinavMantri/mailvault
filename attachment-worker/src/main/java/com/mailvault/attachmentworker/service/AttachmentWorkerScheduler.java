package com.mailvault.attachmentworker.service;

import com.mailvault.attachmentworker.repository.AttachmentRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AttachmentWorkerScheduler {

    private static final int BATCH_SIZE = 50;

    private final AttachmentRepository attachmentRepository;
    private final AttachmentProcessingService attachmentProcessingService;

    public AttachmentWorkerScheduler(
            AttachmentRepository attachmentRepository,
            AttachmentProcessingService attachmentProcessingService
    ) {
        this.attachmentRepository = attachmentRepository;
        this.attachmentProcessingService = attachmentProcessingService;
    }

    @Scheduled(fixedDelayString = "${mailvault.worker.poll-delay-ms:1000}")
    public void processUploadedAttachments() {
        attachmentRepository.findUploaded(BATCH_SIZE)
                .forEach(attachmentProcessingService::process);
    }
}
