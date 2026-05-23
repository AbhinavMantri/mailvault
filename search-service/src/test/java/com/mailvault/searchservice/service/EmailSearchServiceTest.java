package com.mailvault.searchservice.service;

import com.mailvault.searchservice.opensearch.EmailSearchClient;
import com.mailvault.searchservice.opensearch.SearchDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailSearchServiceTest {

    @Mock
    private EmailSearchClient emailSearchClient;

    @InjectMocks
    private EmailSearchService emailSearchService;

    @Test
    void searchMapsOpenSearchDocumentsToApiResults() {
        UUID emailId = UUID.randomUUID();
        Instant receivedAt = Instant.parse("2026-05-23T12:00:00Z");
        when(emailSearchClient.search("user-123", "invoice", 20))
                .thenReturn(List.of(new SearchDocument(
                        emailId,
                        "user-123",
                        "billing@example.com",
                        List.of("abhinav@example.com"),
                        "Invoice for May",
                        2048L,
                        receivedAt
                )));

        var results = emailSearchService.search("user-123", "invoice", 20);

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().emailId()).isEqualTo(emailId);
        assertThat(results.getFirst().subject()).isEqualTo("Invoice for May");
        assertThat(results.getFirst().recipients()).containsExactly("abhinav@example.com");
        assertThat(results.getFirst().receivedAt()).isEqualTo(receivedAt);
    }
}
