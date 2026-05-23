# Mailbox Service

Read-side service for mailbox APIs.

## Responsibilities

- List a user's inbox from finalized email metadata.
- Return email detail including recipients and attachment metadata.
- Return storage usage for quota visibility.

This service is intentionally read-only in the current phase. Schema ownership remains with `ingestion-service` until migrations are extracted into a shared database module.

## Run Locally

From the repository root:

```bash
docker compose up -d
```

Then start the service:

```bash
cd mailbox-service
mvn spring-boot:run
```

Health check:

```bash
curl http://localhost:8082/actuator/health
```

## APIs

List inbox items:

```bash
curl "http://localhost:8082/mailboxes/user-123/inbox?limit=20"
```

Read an email detail record:

```bash
curl "http://localhost:8082/emails/{emailId}?userId=user-123"
```

Read storage usage:

```bash
curl http://localhost:8082/users/user-123/storage
```
