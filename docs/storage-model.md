# Storage Model

## Postgres

Stores durable structured state:

- emails
- recipients
- attachments
- email_attachment_refs
- storage_usage
- outbox_events

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
