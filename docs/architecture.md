# Architecture

MailVault separates the email system into independent storage and processing responsibilities.

Editable Draw.io source: [architecture.drawio](architecture.drawio)

## Diagrams

- [Main HLD](mailvault-hld.svg): high-level system view for README and portfolio scanning.
- [Business Use Cases](diagrams/business-use-cases.svg): user, system, and operations scenarios that explain why the platform exists.
- [Attachment Security Flow](diagrams/attachment-security-flow.svg): attachment hashing, deduplication, antivirus scanning, and quarantine behavior.
- [Storage Lifecycle Flow](diagrams/storage-lifecycle-flow.svg): hot storage, archival movement, search continuity, and restore behavior.

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
  -> Publish email.received event
  -> Return accepted response
```

The write path blocks only on work required to safely accept the email: validation, quota reservation, object storage, metadata persistence, and event handoff. Search indexing, archival, antivirus scanning, deduplication, and quota reconciliation remain asynchronous.

## Read Path

Mailbox list and email detail APIs read from Postgres. Quota APIs read storage usage from `quota-service`. Full-text search reads from OpenSearch and resolves canonical message state from Postgres when needed.

Attachment metadata extraction and hash-based deduplication run asynchronously. Postgres keeps attachment references and content hashes, while MinIO stores the physical object content.

Attachments are not considered downloadable until the security scan records a clean verdict. Unsafe attachments are marked quarantined and excluded from download paths.

## Consistency Model

Email metadata is strongly persisted before the import request succeeds. Search is eventually consistent because indexing is asynchronous.

## Failure Modes

- If OpenSearch is unavailable, email ingestion should continue.
- If Kafka publish fails after metadata persistence, an outbox table can be used to recover event delivery.
- If MinIO storage fails, ingestion should fail before metadata is committed.
- If quota reservation fails, ingestion should reject the import before writing objects.
- If quota reservation succeeds but later storage or metadata persistence fails, a compensation path should release the reserved bytes.
