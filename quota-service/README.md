# Quota Service

Owns quota and storage usage APIs for MailVault.

## Responsibilities

- Reserve logical storage for accepted imports.
- Return per-user logical storage usage.
- Calculate usage percentage against quota.
- Provide the boundary for future quota reconciliation and plan-based quota limits.

`ingestion-service` calls this service before storing an imported email. The reservation uses a database row lock to avoid concurrent imports overshooting the user's quota.

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

Reserve quota:

```bash
curl -X POST http://localhost:8083/quota/reservations \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user-123",
    "bytes": 1048576
  }'
```

Read storage usage:

```bash
curl http://localhost:8083/users/user-123/storage
```
