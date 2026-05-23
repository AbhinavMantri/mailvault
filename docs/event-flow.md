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
  -> write outbox event
  -> publish email.received
```

## Worker Flow

```text
email.received
  -> search-indexer indexes body, subject, sender, recipients, labels
  -> quota-service updates usage
  -> attachment worker extracts metadata
```

## Reliability Notes

Workers should be idempotent. Event handlers should tolerate duplicate delivery by checking stable IDs such as `emailId`, `attachmentId`, or `eventId`.

