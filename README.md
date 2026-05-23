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
- Store email metadata in Postgres. _Initial schema added._
- Store raw content and attachments in MinIO. _Implemented for email bodies and Base64 attachments._
- Publish email events to Kafka. _Implemented for `email.received`._
- Index searchable fields in OpenSearch.
- Search by sender, recipient, subject, body, date, and labels.
- Track per-user storage quota. _Initial logical usage tracking added._
- Deduplicate attachments using SHA-256 hashes. _Initial content-hash dedupe added._
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

Import a sample email:

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
    "attachments": [
      {
        "filename": "invoice.txt",
        "contentType": "text/plain",
        "base64Content": "SGVsbG8gTWFpbFZhdWx0"
      }
    ]
  }'
```

Local endpoints:

| Component | URL |
| --- | --- |
| Ingestion service | `http://localhost:8081` |
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
| `mailbox-service` | Serve inbox, message detail, labels, archive, delete, and search APIs |
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

Phase 1 foundation in progress. The repository now includes local infrastructure and the first runnable service: `ingestion-service`.

Implemented so far:

- Docker Compose infrastructure for Postgres, Kafka, MinIO, and OpenSearch
- Spring Boot `ingestion-service`
- `POST /emails/import`
- Flyway schema for emails, recipients, attachments, attachment references, storage usage, and outbox events
- MinIO object writes for raw email/body/attachments
- SHA-256 attachment deduplication
- Kafka `email.received` event publication
- Actuator health endpoint
