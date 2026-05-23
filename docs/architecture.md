# Architecture

MailVault separates the email system into independent storage and processing responsibilities.

## Core Principle

Postgres is the source of truth for structured mailbox state. MinIO stores large immutable content. Kafka decouples non-critical work. OpenSearch serves fast full-text queries as a derived index.

## Write Path

```text
Client
  -> Ingestion Service
  -> Validate request
  -> Store raw content and attachments in MinIO
  -> Save metadata in Postgres
  -> Publish email.received event
  -> Return accepted response
```

The write path should avoid blocking on search indexing, archival, or expensive attachment processing.

## Read Path

Mailbox list and email detail APIs read from Postgres. Full-text search reads from OpenSearch and resolves canonical message state from Postgres when needed.

## Consistency Model

Email metadata is strongly persisted before the import request succeeds. Search is eventually consistent because indexing is asynchronous.

## Failure Modes

- If OpenSearch is unavailable, email ingestion should continue.
- If Kafka publish fails after metadata persistence, an outbox table can be used to recover event delivery.
- If MinIO storage fails, ingestion should fail before metadata is committed.
- If quota calculation lags, the system should use a conservative stored usage value before accepting large imports.

