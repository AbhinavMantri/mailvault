# Event Flow

## Events

```text
email.received
email.index.requested
email.indexed
quota.recalculation.requested
quota.updated
email.archival.requested
email.archived
```

## Ingestion Flow

```text
POST /emails/import
  -> store objects
  -> persist metadata
  -> write email.received outbox event in the same transaction
  -> scheduled outbox publisher sends email.received to Kafka
```

Production Kubernetes direction:

```text
POST /emails/import
  -> persist metadata and outbox_events in Postgres
  -> Debezium reads committed outbox rows from Postgres WAL
  -> Kafka Connect publishes email.received to Kafka
```

## Worker Flow

```text
email.received
  -> search-indexer indexes subject, sender, recipients, user, and timestamps
  -> Redis filters recent duplicate events; OpenSearch upsert by emailId remains the durable fallback
  -> archival-worker evaluates lifecycle rules

attachment-worker
  -> polls UPLOADED attachments
  -> computes SHA-256 from object storage bytes
  -> creates or reuses canonical attachment_blobs row
  -> deletes temporary pending object
  -> marks attachments READY or FAILED

quota-service
  -> consumes email.received
  -> records eventId in storage_usage_events
  -> updates logical storage_usage asynchronously
  -> may publish quota.updated later for projections and observability
```

## Reliability Notes

Workers should be idempotent. Event handlers should tolerate duplicate delivery by checking stable IDs such as `emailId`, `attachmentId`, or `eventId`.
