# Ingestion Service TODO

Hardening items to complete before treating ingestion-service as production-ready.

## Reliability

- Add idempotency keys for `POST /emails/import`.
- Add idempotency behavior for `POST /attachments/{attachmentId}/complete`.
- Add correlation IDs and structured request logs.
- Add retry/backoff metrics and alerts for stuck unpublished outbox rows.
- Add Debezium/Kafka Connect outbox connector configuration for Kubernetes production deployment.

## Attachment Upload

- Enforce max attachment size, initially 25 MB.
- Enforce max attachments per email.
- Validate allowed/blocked content types.
- Verify object existence and expected size before marking an attachment `UPLOADED`.
- Publish `attachment.uploaded` after upload completion.

## Quota

- Add optional quota pre-check before accepting very large imports.
- Decide whether strict quota enforcement should hold/reject imported email after async quota accounting detects overage.

## Testing

- Add controller tests for validation and response codes.
- Add Testcontainers integration tests for Postgres, MinIO, and Kafka.
- Add failure-path tests for object storage.

## Security

- Add asynchronous antivirus scanning.
- Block attachment download until scan verdict is clean.
- Quarantine infected attachments.
