# Storage Model

## Postgres

Stores durable structured state:

- emails
- recipients
- attachments
- email_attachment_refs
- storage_usage
- storage_usage_events
- outbox_events

`storage_usage` and `storage_usage_events` are owned by `quota-service`. The tables are still created by the shared local-development schema, but application writes should come from quota-service's event consumer rather than ingestion or mailbox code.

Planned later:

- users
- mailboxes
- labels

## Object Storage

Stores large immutable content:

- raw MIME message
- normalized text body
- HTML body
- directly uploaded attachments

Suggested object keys:

```text
users/{userId}/emails/{emailId}/raw.eml
users/{userId}/emails/{emailId}/body.txt
users/{userId}/pending-attachments/{attachmentId}/{filename}
attachments/blobs/sha256/{first2}/{sha256}
archive/users/{userId}/emails/{emailId}/raw.eml
```

## Deduplication

Attachments are hashed using SHA-256 by `attachment-worker` after upload completion. The worker stores the hash on the `attachments` row, creates or reuses an `attachment_blobs` row, and points duplicate logical attachments at the same canonical blob.

Physical deduplication uses a canonical blob model:

```text
attachment_blobs
- id
- sha256
- object_key
- size_bytes
- ref_count
```

Many logical attachments can reference one physical object when the content hash matches.

Quota accounting can be configured in two ways:

- physical usage: count deduplicated bytes once globally
- logical usage: count attachment bytes per user reference

For a consumer mailbox product, logical usage is usually easier to explain to users. For infrastructure cost analysis, physical usage is more accurate.

The MVP uses logical usage. `quota-service` consumes `email.received`, records the event in `storage_usage_events` for idempotency, and updates `storage_usage` asynchronously.
