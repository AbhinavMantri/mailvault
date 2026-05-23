# MailVault

Scalable email storage, search, and archival system designed to explore the real storage pressure behind large mailbox platforms.

MailVault is not a Gmail clone. It focuses on the backend architecture required to ingest email, separate metadata from large content, deduplicate attachments, enforce storage quotas, index messages for search, and move old data into cheaper archival storage.

## Problem

Email storage grows continuously because users rarely delete mail, attachments are duplicated across many accounts, search indexes require their own storage, and cloud providers must replicate and back up data for durability.

The goal of MailVault is to demonstrate a production-minded design for this problem:

- Store mailbox metadata in a relational database.
- Store raw MIME messages and attachments in object storage.
- Deduplicate attachments by content hash.
- Process indexing, scanning, quota updates, and archival asynchronously.
- Keep search as a derived read model, not the source of truth.
- Make storage usage and failure modes observable.

## Target Architecture

![MailVault architecture](docs/mailvault-hld.svg)

Editable Draw.io source: [`docs/architecture.drawio`](docs/architecture.drawio)

Focused diagrams:

- [Business Use Cases](docs/diagrams/business-use-cases.svg)
- [Attachment Security Flow](docs/diagrams/attachment-security-flow.svg)
- [Storage Lifecycle Flow](docs/diagrams/storage-lifecycle-flow.svg)

## MVP Scope

- Import email through an API. _Implemented in `ingestion-service`._
- Read inbox, email detail, and storage usage through APIs. _Implemented in `mailbox-service`._
- Store email metadata in Postgres. _Initial schema added._
- Store raw content and attachments in MinIO. _Implemented with direct attachment upload URLs._
- Publish email events to Kafka. _Implemented for `email.received`._
- Index searchable fields in OpenSearch.
- Search by sender, recipient, subject, body, date, and labels.
- Track per-user storage quota. _Initial logical usage tracking added._
- Deduplicate attachments using SHA-256 hashes. _Planned in async attachment worker; upload lifecycle is implemented._
- Run the full stack locally with Docker Compose. _Infrastructure Compose file added._

## Local Development

Start the local infrastructure:

```bash
docker compose up -d
```

Run the ingestion service:

```bash
cd ingestion-service
mvn spring-boot:run
```

Run the mailbox service in another terminal:

```bash
cd mailbox-service
mvn spring-boot:run
```

Initiate an attachment upload:

```bash
curl -X POST http://localhost:8081/attachments/initiate \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-123",
    "filename": "invoice.txt",
    "contentType": "text/plain",
    "sizeBytes": 15
  }'
```

Upload the file directly to the returned `uploadUrl`, then complete it:

```bash
curl -X POST http://localhost:8081/attachments/{attachmentId}/complete
```

Import a sample email using uploaded attachment IDs:

```bash
curl -X POST http://localhost:8081/emails/import \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-123",
    "from": "billing@example.com",
    "to": ["abhinav@example.com"],
    "subject": "Invoice for May",
    "textBody": "Invoice attached.",
    "htmlBody": "<p>Invoice attached.</p>",
    "attachmentIds": ["00000000-0000-0000-0000-000000000000"]
  }'
```

Local endpoints:

| Component | URL |
| --- | --- |
| Ingestion service | `http://localhost:8081` |
| Mailbox service | `http://localhost:8082` |
| MinIO console | `http://localhost:9001` |
| OpenSearch | `http://localhost:9200` |
| Postgres | `localhost:5432` |
| Kafka | `localhost:9092` |

## Production Hardening Roadmap

- Add asynchronous antivirus scanning for attachments.
- Block attachment downloads until a clean scan verdict exists.
- Quarantine infected attachments and exclude them from download paths.
- Add retry, timeout, and alerting behavior for scan failures.

## Planned Services

| Service | Responsibility |
| --- | --- |
| `ingestion-service` | Accept email imports, persist metadata, store raw content, publish events |
| `mailbox-service` | Serve inbox, message detail, and storage usage read APIs |
| `search-indexer` | Consume indexing events and update OpenSearch |
| `attachment-worker` | Extract attachment metadata, compute content hashes, and support deduplication |
| `attachment-scanner` | Scan attachments asynchronously and record clean, infected, or failed verdicts |
| `quota-service` | Track user storage usage and enforce quota decisions |
| `archival-worker` | Move old email content to archival object-storage prefixes |

## Engineering Themes

- Hybrid storage design
- Event-driven processing
- Idempotent ingestion
- Attachment deduplication
- Search-index consistency
- Quota enforcement
- Lifecycle and archival policies
- Operational observability

## Status

Phase 1 foundation in progress. The repository now includes local infrastructure plus the first write-side and read-side services.

Implemented so far:

- Docker Compose infrastructure for Postgres, Kafka, MinIO, and OpenSearch
- Spring Boot `ingestion-service`
- Spring Boot `mailbox-service`
- `POST /attachments/initiate` for presigned upload URLs
- `POST /attachments/{attachmentId}/complete`
- `POST /emails/import`
- `GET /mailboxes/{userId}/inbox`
- `GET /emails/{emailId}?userId={userId}`
- `GET /users/{userId}/storage`
- Flyway schema for emails, recipients, attachments, attachment references, storage usage, and outbox events
- MinIO object writes for raw email/body and direct attachment uploads
- uploaded attachment references on email import
- Kafka `email.received` event publication
- Actuator health endpoint
