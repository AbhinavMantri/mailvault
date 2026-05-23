package com.mailvault.searchservice.service;

import com.mailvault.searchservice.api.SearchResultResponse;
import com.mailvault.searchservice.opensearch.EmailSearchClient;
import com.mailvault.searchservice.opensearch.SearchDocument;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmailSearchService {

    private final EmailSearchClient emailSearchClient;

    public EmailSearchService(EmailSearchClient emailSearchClient) {
        this.emailSearchClient = emailSearchClient;
    }

    public List<SearchResultResponse> search(String userId, String query, int limit) {
        return emailSearchClient.search(userId, query, limit).stream()
                .map(this::toResponse)
                .toList();
    }

    private SearchResultResponse toResponse(SearchDocument document) {
        return new SearchResultResponse(
                document.emailId(),
                document.sender(),
                document.recipients(),
                document.subject(),
                document.receivedAt(),
                document.logicalSizeBytes()
        );
    }
}
