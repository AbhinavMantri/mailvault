package com.mailvault.searchindexer.service;

import com.mailvault.searchindexer.events.EmailReceivedEvent;
import com.mailvault.searchindexer.idempotency.EventIdempotencyCache;
import com.mailvault.searchindexer.search.EmailSearchDocument;
import com.mailvault.searchindexer.search.OpenSearchEmailIndexer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchIndexingServiceTest {

    @Mock
    private OpenSearchEmailIndexer emailIndexer;

    @Mock
    private EventIdempotencyCache idempotencyCache;

    @InjectMocks
    private SearchIndexingService searchIndexingService;

    @Test
    void indexEmailMapsEventToSearchDocument() {
        UUID eventId = UUID.randomUUID();
        UUID emailId = UUID.randomUUID();
        Instant receivedAt = Instant.parse("2026-05-23T12:00:00Z");
        EmailReceivedEvent event = new EmailReceivedEvent(
                eventId,
                emailId,
                "user-123",
                "billing@example.com",
                List.of("abhinav@example.com"),
                "Invoice for May",
                2048L,
                receivedAt
        );
        when(idempotencyCache.wasRecentlyProcessed("search-indexer", "email.received", eventId)).thenReturn(false);

        searchIndexingService.indexEmail(event);

        ArgumentCaptor<EmailSearchDocument> captor = ArgumentCaptor.forClass(EmailSearchDocument.class);
        verify(emailIndexer).upsert(captor.capture());
        assertThat(captor.getValue().emailId()).isEqualTo(emailId);
        assertThat(captor.getValue().userId()).isEqualTo("user-123");
        assertThat(captor.getValue().subject()).isEqualTo("Invoice for May");
        assertThat(captor.getValue().recipients()).containsExactly("abhinav@example.com");
        assertThat(captor.getValue().logicalSizeBytes()).isEqualTo(2048L);
        assertThat(captor.getValue().receivedAt()).isEqualTo(receivedAt);
        verify(idempotencyCache).rememberProcessed("search-indexer", "email.received", eventId);
    }

    @Test
    void indexEmailSkipsRecentlyProcessedEvent() {
        UUID eventId = UUID.randomUUID();
        EmailReceivedEvent event = new EmailReceivedEvent(
                eventId,
                UUID.randomUUID(),
                "user-123",
                "billing@example.com",
                List.of("abhinav@example.com"),
                "Invoice for May",
                2048L,
                Instant.parse("2026-05-23T12:00:00Z")
        );
        when(idempotencyCache.wasRecentlyProcessed("search-indexer", "email.received", eventId)).thenReturn(true);

        searchIndexingService.indexEmail(event);

        verify(emailIndexer, never()).upsert(any());
        verify(idempotencyCache, never()).rememberProcessed(any(), any(), any());
    }
}
