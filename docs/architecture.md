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
  -> Store raw content and attachments in MinIO
  -> Save metadata in Postgres
  -> Write email.received outbox event
  -> Return accepted response
```

The write path blocks only on work required to safely accept the email: validation, object storage, metadata persistence, and event handoff. Search indexing, quota accounting, archival, antivirus scanning, and deduplication remain asynchronous.

## Read Path

Mailbox list and email detail APIs read from Postgres. Quota APIs read storage usage from `quota-service`. `search-indexer` consumes `email.received` events and writes searchable email fields into OpenSearch. `quota-service` also consumes `email.received` and updates logical storage usage asynchronously. `search-service` serves user search queries from OpenSearch and can later resolve canonical message state from Postgres when needed.

Attachment metadata extraction and SHA-256 hash calculation run asynchronously in `attachment-worker` after `attachment.uploaded` is published. Postgres keeps logical attachment rows and canonical `attachment_blobs` rows, while MinIO stores both temporary pending uploads and canonical SHA-256 blob objects. Duplicate attachment content reuses the same canonical blob after hashing.

Attachments are not considered downloadable until the security scan records a clean verdict. Unsafe attachments are marked quarantined and excluded from download paths.

## Thread And Recipient Model

An email is the immutable message/content unit. A thread is the user-visible conversation container. Attachments and recipients belong to individual emails, not directly to the thread.

For the first imported message, ingestion creates the message and a first `user_threads` row for that user. The `thread_messages` row links the message to the thread with a direction such as `INBOUND`. Later reply/send APIs can append more `thread_messages` rows to the same thread.

`TO`, `CC`, and `BCC` are stored in `email_recipients.recipient_type`. Response shaping for BCC visibility is a later authorization concern: the sender can see all BCC recipients, normal TO/CC recipients should not see BCC recipients, and a BCC recipient should only see their own BCC participation.

Current folder placement is held on `user_threads.folder` for mailbox views such as `INBOX`, `SPAM`, `TRASH`, and `ARCHIVE`. `SENT` is better treated as a message-level view filtered by `thread_messages.direction = OUTBOUND`.

## Mailbox Safety

Spam filtering and abuse reporting are separate workflows. Spam filtering is system-driven classification. Abuse reporting is a user-triggered complaint that should be recorded durably for audit, repeated-sender analysis, and possible moderation action.

Planned mailbox safety model:

```text
email_reports
- id
- email_id
- reporter_user_id
- reported_sender
- reason: SPAM | PHISHING | ABUSE | HARASSMENT | IMPERSONATION | OTHER
- description
- status: OPEN | REVIEWED | ACTIONED | REJECTED
- created_at
```

Reporting an email can apply a per-user `REPORTED` or `SPAM` label immediately, while moderation remains asynchronous. Repeated reports can later update sender/domain risk scoring without blocking normal mailbox reads.

## Consistency Model

Email metadata is strongly persisted before the import request succeeds. Search is eventually consistent because indexing is asynchronous.

`ingestion-service` writes `email.received` into `outbox_events` in the same database transaction as email metadata. The MVP publishes those rows with a scheduled outbox publisher. In a Kubernetes production deployment, the preferred evolution is Debezium CDC through Kafka Connect, where the connector reads committed outbox rows from the Postgres WAL and publishes them to Kafka.

Consumers are idempotent by service-specific durable state, not by one global processed-events table. Redis is used only as a recent duplicate filter. If Redis misses or is unavailable, consumers fall back to safe writes such as OpenSearch upsert by `emailId`, conditional status transitions, or business ledgers with unique event IDs.

## Failure Modes

- If OpenSearch is unavailable, email ingestion should continue.
- If Kafka publish fails after metadata persistence, the outbox row remains durable. The MVP scheduled publisher can retry it; the production CDC path would rely on Debezium/Kafka Connect offsets and retries.
- If MinIO storage fails, ingestion should fail before metadata is committed.
- If quota-service is unavailable, ingestion can still accept email and quota usage catches up from Kafka later.
- Strict quota enforcement can be added later with a pre-check or post-import hold/reject workflow.
