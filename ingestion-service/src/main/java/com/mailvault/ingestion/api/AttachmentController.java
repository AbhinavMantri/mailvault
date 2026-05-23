package com.mailvault.ingestion.api;

import com.mailvault.ingestion.api.dto.AttachmentCompleteResponse;
import com.mailvault.ingestion.api.dto.AttachmentInitiateRequest;
import com.mailvault.ingestion.api.dto.AttachmentInitiateResponse;
import com.mailvault.ingestion.service.AttachmentUploadService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/attachments")
public class AttachmentController {

    private final AttachmentUploadService attachmentUploadService;

    public AttachmentController(AttachmentUploadService attachmentUploadService) {
        this.attachmentUploadService = attachmentUploadService;
    }

    @PostMapping("/initiate")
    public AttachmentInitiateResponse initiateUpload(@Valid @RequestBody AttachmentInitiateRequest request) {
        return attachmentUploadService.initiateUpload(request);
    }

    @PostMapping("/{attachmentId}/complete")
    public AttachmentCompleteResponse completeUpload(@PathVariable UUID attachmentId) {
        return attachmentUploadService.completeUpload(attachmentId);
    }
}

