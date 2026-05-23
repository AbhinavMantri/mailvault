package com.mailvault.ingestion.api;

import com.mailvault.ingestion.api.dto.EmailImportRequest;
import com.mailvault.ingestion.api.dto.EmailImportResponse;
import com.mailvault.ingestion.service.EmailIngestionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/emails")
public class EmailImportController {

    private final EmailIngestionService emailIngestionService;

    public EmailImportController(EmailIngestionService emailIngestionService) {
        this.emailIngestionService = emailIngestionService;
    }

    @PostMapping("/import")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public EmailImportResponse importEmail(@Valid @RequestBody EmailImportRequest request) {
        return emailIngestionService.importEmail(request);
    }
}

