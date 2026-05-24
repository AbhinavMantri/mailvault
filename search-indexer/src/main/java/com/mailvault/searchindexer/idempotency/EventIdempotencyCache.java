package com.mailvault.searchindexer.idempotency;

import java.util.UUID;

public interface EventIdempotencyCache {

    boolean wasRecentlyProcessed(String consumerName, String eventType, UUID eventId);

    void rememberProcessed(String consumerName, String eventType, UUID eventId);
}
