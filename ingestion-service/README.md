# Ingestion Service

Accepts email imports, creates presigned upload URLs for attachments, stores raw email content in MinIO, persists mailbox metadata in Postgres, updates logical storage usage, and publishes an `email.received` Kafka event.

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

Marks an uploaded attachment as `UPLOADED` so it can be referenced by email import.

```http
POST /emails/import
```

The email import request accepts metadata, body content, and uploaded `attachmentIds`. Attachment bytes do not pass through the email import API. SMTP ingestion is intentionally deferred until the storage path is stable.
