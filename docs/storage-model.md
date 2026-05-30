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
- mailbox_thread_labels

`storage_usage` and `storage_usage_events` are owned by `quota-service`. The tables are still created by the shared local-development schema, but application writes should come from quota-service's event consumer rather than ingestion or mailbox code.

Planned later:

- users
- mailboxes

`mailbox_thread_labels` stores per-user labels on mailbox threads. Labels are metadata, not placement. A thread can stay in `INBOX` while also carrying labels such as `IMPORTANT`, `FAVORITE`, `PROMOTION`, or a custom user label.

Labels include a `source` so manual and system-driven classification can coexist:

```text
USER   - explicitly applied by the mailbox user
SYSTEM - deterministic platform rules
AI     - classifier-generated label with optional confidence_score
```

The current API writes `USER` labels only. `SYSTEM` and `AI` labels are schema-ready for a later classification pipeline.

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

Attachments are hashed using SHA-256 by `attachment-worker` after it consumes `attachment.uploaded`. The worker stores the hash on the `attachments` row, creates or reuses an `attachment_blobs` row, and points duplicate logical attachments at the same canonical blob.

## Compression Policy

The MVP stores attachment bytes as uploaded. It does not compress every attachment before writing the canonical blob because many common attachment formats are already compressed, including PDFs, images, videos, ZIP files, and Office documents. Blanket compression would add CPU cost to the attachment-worker path while often producing little or no storage reduction.

Selective compression can be added later for content types where it is likely to help, such as plain text, CSV, JSON, XML, logs, raw MIME, or large plain HTML. The storage metadata should record whether a canonical blob is compressed and which algorithm was used before enabling downloads for compressed objects.

The current optimization priority is:

1. Deduplicate attachments by SHA-256.
2. Move old content to cheaper archive prefixes or storage classes.
3. Add selective compression only for compressible content types.

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
