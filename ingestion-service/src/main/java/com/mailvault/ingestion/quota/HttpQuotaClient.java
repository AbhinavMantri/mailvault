package com.mailvault.ingestion.quota;

import com.mailvault.ingestion.config.QuotaProperties;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class HttpQuotaClient implements QuotaClient {

    private final RestClient restClient;

    public HttpQuotaClient(RestClient.Builder builder, QuotaProperties properties) {
        this.restClient = builder.baseUrl(properties.baseUrl()).build();
    }

    @Override
    public void reserve(String userId, long bytes) {
        restClient.post()
                .uri("/quota/reservations")
                .body(new QuotaReservationRequest(userId, bytes))
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    throw new IllegalArgumentException("User storage quota exceeded");
                })
                .toBodilessEntity();
    }
}
