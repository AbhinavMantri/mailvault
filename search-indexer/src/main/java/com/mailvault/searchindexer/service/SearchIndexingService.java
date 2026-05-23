package com.mailvault.searchindexer.service;

import com.mailvault.searchindexer.events.EmailReceivedEvent;
import com.mailvault.searchindexer.search.EmailSearchDocument;
import com.mailvault.searchindexer.search.OpenSearchEmailIndexer;
import org.springframework.stereotype.Service;

@Service
public class SearchIndexingService {

    private final OpenSearchEmailIndexer emailIndexer;

    public SearchIndexingService(OpenSearchEmailIndexer emailIndexer) {
        this.emailIndexer = emailIndexer;
    }

    public void indexEmail(EmailReceivedEvent event) {
        emailIndexer.upsert(new EmailSearchDocument(
                event.emailId(),
                event.userId(),
                event.sender(),
                event.recipients(),
                event.subject(),
                event.logicalSizeBytes(),
                event.receivedAt()
        ));
    }
}
