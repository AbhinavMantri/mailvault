# Quota Service

Owns quota and storage usage APIs for MailVault.

## Responsibilities

- Consume `email.received` events from Kafka.
- Update per-user logical storage usage asynchronously.
- Record storage usage events idempotently by `eventId`.
- Return per-user logical storage usage.
- Calculate usage percentage against quota.
- Provide the boundary for future quota reconciliation and plan-based quota limits.

`ingestion-service` does not call quota-service during email import. It publishes `email.received`; quota-service consumes that event and updates `storage_usage`. Duplicate events are ignored through the `storage_usage_events` ledger.

## Run Locally

From the repository root:

```bash
docker compose up -d
```

Then start the service:

```bash
cd quota-service
mvn spring-boot:run
```

Health check:

```bash
curl http://localhost:8083/actuator/health
```

## APIs

Read storage usage:

```bash
curl http://localhost:8083/users/user-123/storage
```
