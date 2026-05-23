package com.mailvault.searchindexer.events;

import com.mailvault.searchindexer.service.SearchIndexingService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class EmailReceivedConsumer {

    private final SearchIndexingService searchIndexingService;

    public EmailReceivedConsumer(SearchIndexingService searchIndexingService) {
        this.searchIndexingService = searchIndexingService;
    }

    @KafkaListener(
            topics = "${mailvault.kafka.email-received-topic}",
            groupId = "${mailvault.kafka.consumer-group-id}"
    )
    public void onEmailReceived(EmailReceivedEvent event) {
        searchIndexingService.indexEmail(event);
    }
}
