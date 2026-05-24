# Architecture

MailVault separates the email system into independent storage and processing responsibilities.

Editable Draw.io source: [architecture.drawio](architecture.drawio)

## Diagrams

- [Main HLD](mailvault-hld.svg): high-level system view for README and portfolio scanning.
- [Business Use Cases](diagrams/business-use-cases.svg): user, system, and operations scenarios that explain why the platform exists.
- [Attachment Security Flow](diagrams/attachment-security-flow.svg): attachment hashing, deduplication, antivirus scanning, and quarantine behavior.
- [Storage Lifecycle Flow](diagrams/storage-lifecycle-flow.svg): hot storage, archival movement, search continuity, and restore behavior.
- [Outbox and CDC Strategy](outbox-cdc.md): reliable event publication path and Debezium production direction.
- [Consumer Idempotency Strategy](consumer-idempotency.md): Redis duplicate filter plus durable target-store fallback.

## Core Principle

Postgres is the source of truth for structured mailbox state. MinIO stores large immutable content. Kafka decouples non-critical work. OpenSearch serves fast full-text queries as a derived index.

## Write Path

```text
Client
  -> Ingestion Service
  -> Validate request
  -> Reserve logical storage in Quota Service
  -> Store raw content and attachments in MinIO
  -> Save metadata in Postgres
  -> Write email.received outbox event
  -> Return accepted response
```

The write path blocks only on work required to safely accept the email: validation, quota reservation, object storage, metadata persistence, and event handoff. Search indexing, archival, antivirus scanning, deduplication, and quota reconciliation remain asynchronous.

## Read Path

Mailbox list and email detail APIs read from Postgres. Quota APIs read storage usage from `quota-service`. `search-indexer` consumes `email.received` events and writes searchable email fields into OpenSearch. `search-service` serves user search queries from OpenSearch and can later resolve canonical message state from Postgres when needed.

Attachment metadata extraction and SHA-256 hash calculation run asynchronously in `attachment-worker`. Postgres keeps logical attachment rows and canonical `attachment_blobs` rows, while MinIO stores both temporary pending uploads and canonical SHA-256 blob objects. Duplicate attachment content reuses the same canonical blob after hashing.

Attachments are not considered downloadable until the security scan records a clean verdict. Unsafe attachments are marked quarantined and excluded from download paths.

## Consistency Model

Email metadata is strongly persisted before the import request succeeds. Search is eventually consistent because indexing is asynchronous.

`ingestion-service` writes `email.received` into `outbox_events` in the same database transaction as email metadata. The MVP publishes those rows with a scheduled outbox publisher. In a Kubernetes production deployment, the preferred evolution is Debezium CDC through Kafka Connect, where the connector reads committed outbox rows from the Postgres WAL and publishes them to Kafka.

Consumers are idempotent by service-specific durable state, not by one global processed-events table. Redis is used only as a recent duplicate filter. If Redis misses or is unavailable, consumers fall back to safe writes such as OpenSearch upsert by `emailId`, conditional status transitions, or business ledgers with unique event IDs.

## Failure Modes

- If OpenSearch is unavailable, email ingestion should continue.
- If Kafka publish fails after metadata persistence, the outbox row remains durable. The MVP scheduled publisher can retry it; the production CDC path would rely on Debezium/Kafka Connect offsets and retries.
- If MinIO storage fails, ingestion should fail before metadata is committed.
- If quota reservation fails, ingestion should reject the import before writing objects.
- If quota reservation succeeds but later storage or metadata persistence fails, a compensation path should release the reserved bytes.
