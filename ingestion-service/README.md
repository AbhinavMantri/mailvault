# Ingestion Service

Accepts email imports, creates presigned upload URLs for attachments, stores raw email content in MinIO, persists mailbox metadata in Postgres, and writes an `email.received` outbox event for reliable Kafka publication.

## Run Locally

From the repository root:

```bash
docker compose up -d
```

Then start the service:

```bash
cd ingestion-service
mvn spring-boot:run
```

Health check:

```bash
curl http://localhost:8081/actuator/health
```

## API

```http
POST /attachments/initiate
```

Creates an attachment record in `PENDING_UPLOAD` state and returns a presigned object-storage upload URL.

```http
POST /attachments/{attachmentId}/complete
```

Marks an uploaded attachment as `UPLOADED` and writes an `attachment.uploaded` outbox event for Kafka publication. Email import accepts attachments in `UPLOADED`, `PROCESSING`, or `READY` state because the worker may process the attachment before the user sends the email.

```http
POST /emails/import
```

The email import request accepts metadata, body content, and uploaded `attachmentIds`. Attachment bytes do not pass through the email import API. SMTP ingestion is intentionally deferred until the storage path is stable.

## Outbox

Email import persists mailbox metadata and the `email.received` outbox event in the same database transaction. A scheduled publisher reads unpublished outbox rows, sends them to Kafka, and marks each row published only after Kafka accepts the send.

For Kubernetes production deployment, the preferred evolution is Debezium CDC with Kafka Connect reading committed `outbox_events` rows from Postgres WAL. The scheduled publisher keeps the MVP runnable locally without requiring Kafka Connect.
