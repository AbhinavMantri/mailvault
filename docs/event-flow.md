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
  -> reserve quota through quota-service
  -> store objects
  -> persist metadata
  -> write outbox event
  -> publish email.received
```

## Worker Flow

```text
email.received
  -> search-indexer indexes subject, sender, recipients, user, and timestamps
  -> archival-worker evaluates lifecycle rules

attachment-worker
  -> polls UPLOADED attachments
  -> computes SHA-256 from object storage bytes
  -> marks attachments READY or FAILED

quota-service
  -> records quota reservation during import
  -> may publish quota.updated for projections and observability
```

## Reliability Notes

Workers should be idempotent. Event handlers should tolerate duplicate delivery by checking stable IDs such as `emailId`, `attachmentId`, or `eventId`.
