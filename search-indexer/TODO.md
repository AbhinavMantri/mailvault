# Search Indexer TODO

Hardening items before treating `search-indexer` as production-ready.

## Index Coverage

- Read normalized body content and include body text in the OpenSearch document.
- Add labels, attachment metadata, and archive state when those services are implemented.
- Create index mappings explicitly instead of relying on dynamic mapping.

## Reliability

- Add retry/backoff behavior for transient OpenSearch failures.
- Add dead-letter topic handling for malformed events.
- Add metrics for Redis idempotency hits, misses, and unavailable fallback.
- Add structured logs with `eventId`, `emailId`, and Kafka offset.

## Testing

- Add an integration test with Testcontainers for Kafka and OpenSearch.
- Add contract coverage for event deserialization from ingestion-produced JSON.
