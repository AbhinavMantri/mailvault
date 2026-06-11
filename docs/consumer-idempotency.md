# Consumer Idempotency Strategy

Kafka and CDC delivery can produce duplicate events. MailVault consumers therefore use idempotent writes and recent duplicate filtering.

## Rule

Redis is a fast duplicate filter, not the source of truth.

```text
event arrives
  -> check Redis idempotency key
  -> if key exists, skip recent duplicate
  -> if key is missing, check durable state or perform an idempotent write
  -> if already applied, refresh Redis key and skip
  -> if not applied, process safely and set Redis key with TTL
```

If Redis is unavailable or a key expires, the consumer still relies on its durable target store.

## Current MVP

`search-indexer` uses:

- Redis key: `mailvault:idempotency:search-indexer:email.received:{eventId}`
- TTL: 7 days by default
- durable fallback: OpenSearch upsert using `userId:emailId` as the document ID

This means duplicate `email.received` events are skipped quickly when the Redis key exists. If Redis misses, the OpenSearch write remains safe because the same mailbox-visible document overwrites the same `userId:emailId` target.

## Service-Specific Idempotency

Avoid one global `processed_events` table for every event. Each consumer should use the natural idempotency key of the side effect it owns.

```text
search-indexer
  -> OpenSearch upsert by userId:emailId
  -> Redis TTL key reduces recent duplicate noise

attachment-worker
  -> conditional status transitions on attachments
  -> consumes attachment.uploaded events
  -> attachment_blobs has unique sha256

archival-worker
  -> email_archive_state keyed by emailId

quota-service / storage-ledger-service
  -> append ledger rows with unique eventId when auditability is required
  -> quota-service currently uses storage_usage_events(event_id) before updating storage_usage
```

## Retention

Pure dedupe keys do not need to live forever. TTL should match the practical duplicate/replay window, usually aligned with Kafka retention and operational replay expectations.

Business ledgers are different: they may be retained longer because they explain user-visible storage usage and support reporting or audit.
