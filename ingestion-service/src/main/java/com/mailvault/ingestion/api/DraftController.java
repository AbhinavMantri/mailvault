package com.mailvault.ingestion.api;

import com.mailvault.ingestion.api.dto.EmailDraftRequest;
import com.mailvault.ingestion.api.dto.EmailImportResponse;
import com.mailvault.ingestion.api.dto.SendDraftRequest;
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
@RequestMapping("/drafts")
public class DraftController {

    private final EmailIngestionService emailIngestionService;

    public DraftController(EmailIngestionService emailIngestionService) {
        this.emailIngestionService = emailIngestionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public EmailImportResponse createDraft(@Valid @RequestBody EmailDraftRequest request) {
        return emailIngestionService.createDraft(request);
    }

    @PostMapping("/{emailId}/send")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public EmailImportResponse sendDraft(@PathVariable UUID emailId,
                                         @Valid @RequestBody SendDraftRequest request) {
        return emailIngestionService.sendDraft(emailId, request);
    }
}
