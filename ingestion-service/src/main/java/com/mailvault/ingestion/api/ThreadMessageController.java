package com.mailvault.ingestion.api;

import com.mailvault.ingestion.api.dto.EmailImportResponse;
import com.mailvault.ingestion.api.dto.EmailReplyRequest;
import com.mailvault.ingestion.service.EmailIngestionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/threads")
public class ThreadMessageController {

    private final EmailIngestionService emailIngestionService;

    public ThreadMessageController(EmailIngestionService emailIngestionService) {
        this.emailIngestionService = emailIngestionService;
    }

    @PostMapping("/{threadId}/messages")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public EmailImportResponse replyToThread(@PathVariable UUID threadId,
                                             @Valid @RequestBody EmailReplyRequest request) {
        return emailIngestionService.replyToThread(threadId, request);
    }
}
