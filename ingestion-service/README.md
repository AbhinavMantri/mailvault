# Ingestion Service

Accepts email imports, stores raw content and attachments in MinIO, persists mailbox metadata in Postgres, updates logical storage usage, and publishes an `email.received` Kafka event.

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
POST /emails/import
```

The initial implementation accepts JSON input with Base64-encoded attachments. SMTP ingestion is intentionally deferred until the storage path is stable.

