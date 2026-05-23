# Storage Model

## Postgres

Stores durable structured state:

- emails
- recipients
- attachments
- email_attachment_refs
- storage_usage
- outbox_events

`storage_usage` is owned by `quota-service`. The table is still created by the initial schema for local development, but application writes should go through quota APIs rather than ingestion or mailbox code.

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
archive/users/{userId}/emails/{emailId}/raw.eml
```

## Deduplication

Attachments are hashed using SHA-256. The physical object is stored once and referenced by many emails when the content hash matches.

Quota accounting can be configured in two ways:

- physical usage: count deduplicated bytes once globally
- logical usage: count attachment bytes per user reference

For a consumer mailbox product, logical usage is usually easier to explain to users. For infrastructure cost analysis, physical usage is more accurate.

The MVP uses logical usage. `quota-service` reserves logical bytes before ingestion stores a message so concurrent imports cannot overshoot quota.
