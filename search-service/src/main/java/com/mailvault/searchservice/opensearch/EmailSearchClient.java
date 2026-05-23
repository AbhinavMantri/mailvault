package com.mailvault.searchservice.opensearch;

import com.mailvault.searchservice.config.SearchProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class EmailSearchClient {

    private final RestClient restClient;
    private final SearchProperties properties;

    public EmailSearchClient(RestClient.Builder builder, SearchProperties properties) {
        this.restClient = builder.baseUrl(properties.baseUrl()).build();
        this.properties = properties;
    }

    public List<SearchDocument> search(String userId, String query, int limit) {
        SearchResponse response = restClient.post()
                .uri("/{index}/_search", properties.emailIndex())
                .body(searchBody(userId, query, limit))
                .retrieve()
                .body(SearchResponse.class);

        if (response == null || response.hits() == null || response.hits().hits() == null) {
            return List.of();
        }
        return response.hits().hits().stream()
                .map(SearchHit::source)
                .toList();
    }

    private Map<String, Object> searchBody(String userId, String query, int limit) {
        return Map.of(
                "size", limit,
                "query", Map.of(
                        "bool", Map.of(
                                "filter", List.of(Map.of("term", Map.of("userId.keyword", userId))),
                                "must", List.of(Map.of("multi_match", Map.of(
                                        "query", query,
                                        "fields", List.of("subject^3", "sender^2", "recipients")
                                )))
                        )
                ),
                "sort", List.of(Map.of("receivedAt", Map.of("order", "desc")))
        );
    }
}
