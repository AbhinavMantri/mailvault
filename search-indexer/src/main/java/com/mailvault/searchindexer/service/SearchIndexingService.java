package com.mailvault.searchindexer.service;

import com.mailvault.searchindexer.events.EmailReceivedEvent;
import com.mailvault.searchindexer.idempotency.EventIdempotencyCache;
import com.mailvault.searchindexer.search.EmailSearchDocument;
import com.mailvault.searchindexer.search.OpenSearchEmailIndexer;
import org.springframework.stereotype.Service;

@Service
public class SearchIndexingService {

    private static final String CONSUMER_NAME = "search-indexer";
    private static final String EMAIL_RECEIVED = "email.received";

    private final OpenSearchEmailIndexer emailIndexer;
    private final EventIdempotencyCache idempotencyCache;

    public SearchIndexingService(OpenSearchEmailIndexer emailIndexer, EventIdempotencyCache idempotencyCache) {
        this.emailIndexer = emailIndexer;
        this.idempotencyCache = idempotencyCache;
    }

    public void indexEmail(EmailReceivedEvent event) {
        if (idempotencyCache.wasRecentlyProcessed(CONSUMER_NAME, EMAIL_RECEIVED, event.eventId())) {
            return;
        }

        emailIndexer.upsert(new EmailSearchDocument(
                event.emailId(),
                event.userId(),
                event.sender(),
                event.recipients(),
                event.subject(),
                event.logicalSizeBytes(),
                event.receivedAt()
        ));
        idempotencyCache.rememberProcessed(CONSUMER_NAME, EMAIL_RECEIVED, event.eventId());
    }
}
