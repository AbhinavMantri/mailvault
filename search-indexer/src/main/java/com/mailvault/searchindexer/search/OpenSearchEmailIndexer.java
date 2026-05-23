package com.mailvault.searchindexer.search;

import com.mailvault.searchindexer.config.SearchIndexProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OpenSearchEmailIndexer {

    private final RestClient restClient;
    private final SearchIndexProperties properties;

    public OpenSearchEmailIndexer(RestClient.Builder builder, SearchIndexProperties properties) {
        this.restClient = builder.baseUrl(properties.baseUrl()).build();
        this.properties = properties;
    }

    public void upsert(EmailSearchDocument document) {
        restClient.put()
                .uri("/{index}/_doc/{emailId}", properties.emailIndex(), document.emailId())
                .body(document)
                .retrieve()
                .toBodilessEntity();
    }
}
