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

An email is the immutable message/content unit. A canonical `conversation` is the shared conversation container. A `mailbox_thread` is the per-user mailbox view of that conversation, carrying user-specific placement such as folder, unread count, labels, spam/trash/archive state, and last-message summary. Attachments and recipients belong to individual emails, not directly to the thread.

For the first imported message, ingestion creates the message, a canonical `conversations` row, and a first `mailbox_threads` row for that user. The `thread_messages` row links the message to the mailbox thread with a direction such as `INBOUND`. Later reply/send APIs can append more `thread_messages` rows to the same mailbox thread while preserving the shared conversation boundary.

`POST /emails/import` is intentionally treated as a new conversation boundary in the core API. It should not carry complex thread-resolution heuristics. `POST /threads/{threadId}/messages` receives an explicit `threadId`, validates ownership, creates a new `emails` row, and appends a `thread_messages` row to the existing thread.

Outbound APIs also perform MVP local delivery. If a reply, draft send, single-email forward, or full-thread forward targets an address ending in `@mailvault.local`, ingestion maps the address local part to the recipient user ID and creates a recipient-owned `INBOX` mailbox thread that points to the same canonical conversation. The recipient `thread_messages` row links to the same `emails` row as the sender view, with direction `INBOUND`; the sender view links the same email with direction `OUTBOUND`. Body objects and attachment objects are therefore not duplicated, and neither is the canonical email row. `POST /emails/import` does not fan out to local recipients because it is already the inbound acceptance path for one mailbox owner.

`POST /emails/{emailId}/forward` validates that the source email belongs to the user, then creates a new outbound `emails` row and a new sent conversation. Forwarding starts a new MailVault thread in the current MVP because the user is composing a new message to new recipients, not replying inside the original conversation. The forwarded body includes the user's message plus a forwarded-message block with the original sender, subject, and body. The forward can reuse the original logical attachment references when `includeOriginalAttachments = true` and can also attach newly uploaded files through `attachmentIds`; downstream quota and search still flow through the normal `email.received` event path.

`POST /threads/{threadId}/forward` is a separate conversation-level forwarding API. It validates thread ownership, reads ordered `thread_messages`, generates one outbound email containing the user's message plus a forwarded-conversation block, and creates a new sent thread. Keeping this separate from email-level forwarding avoids overloading `POST /emails/{emailId}/forward` and makes the body size, BCC visibility, and attachment policy easier to reason about.

Full-thread forwarding is bounded in the MVP: requests are rejected if the source thread has more than 25 messages or the generated forwarded body exceeds 1 MB. Original attachments are reused through logical attachment references; the object bytes are not copied or re-uploaded.

`delivery-service` is the outbound SMTP boundary. It sends MIME messages through a configured SMTP server and is tested with GreenMail so local development can validate SMTP behavior without touching real external mail infrastructure. Ingestion is still the system of record for accepted messages; wiring ingestion to delivery should happen through an event boundary later, with retries, bounce handling, suppression lists, and provider/reputation safeguards owned by delivery-service rather than the request path.

If MailVault later supports Gmail, Outlook, or `.eml` migration, that should be handled by a separate migration adapter. The adapter can parse external conversation signals such as provider thread IDs, `Message-ID`, `In-Reply-To`, and `References`, then convert the imported history into MailVault's native `mailbox_threads`, `emails`, `thread_messages`, recipients, and attachment refs. That keeps the normal import path simple and keeps external-provider rules outside core ingestion.

`TO`, `CC`, and `BCC` are stored in `email_recipients.recipient_type`. Response shaping for BCC visibility is a later authorization concern: the sender can see all BCC recipients, normal TO/CC recipients should not see BCC recipients, and a BCC recipient should only see their own BCC participation.

Current folder placement is held on `mailbox_threads.folder` for mailbox views such as `ACTIVE`, `INBOX`, `DRAFT`, `SPAM`, `TRASH`, and `ARCHIVE`. `SENT` is a message-level view filtered by `thread_messages.direction = OUTBOUND`, because a thread can contain both inbound and outbound messages.

Drafts are stored as normal `emails` rows with `EmailStatus.DRAFT`, linked to a `mailbox_threads.folder = DRAFT` row through `thread_messages.direction = DRAFT`. Creating a draft does not publish `email.received`. Sending an existing draft is an explicit transition: the same email row moves to `INDEX_PENDING`, the connector row moves to `OUTBOUND`, the thread moves out of `DRAFT`, and the event is published for downstream quota/search processing.

Read state is stored on `thread_messages.read_at`. `INBOUND` messages with `read_at IS NULL` are unread. Thread list reads use `mailbox_threads.unread_count` as a denormalized counter; mark-read updates all unread inbound rows and clears the counter, while mark-unread clears the latest inbound row and sets the counter to one.

Mailbox actions such as archive, trash, spam, and restore operate at thread level by updating `mailbox_threads.folder`. Restore returns threads with inbound messages to `INBOX`; sent-only conversations restore to `ACTIVE`.

Labels are stored separately from folders in `mailbox_thread_labels`. This allows user intent and categories such as `IMPORTANT`, `FAVORITE`, `PROMOTION`, or custom labels to coexist with normal mailbox placement. Label filtering reads from Postgres and returns the same thread summary shape with the current label set attached.

Manual labels are written with `source = USER`. The schema also supports `SYSTEM` and `AI` sources plus optional confidence scores, but automatic classification is intentionally not implemented in the current mailbox-service feature. A later classifier can add `PROMOTION`, `SOCIAL`, `SECURITY_ALERT`, or similar labels asynchronously without overwriting user labels.

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
