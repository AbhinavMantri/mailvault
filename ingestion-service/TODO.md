# Ingestion Service TODO

Hardening items to complete before treating ingestion-service as production-ready.

## Reliability

- Implement transactional outbox publishing instead of direct Kafka publish from the request transaction.
- Add idempotency keys for `POST /emails/import`.
- Add idempotency behavior for `POST /attachments/{attachmentId}/complete`.
- Add correlation IDs and structured request logs.

## Attachment Upload

- Enforce max attachment size, initially 25 MB.
- Enforce max attachments per email.
- Validate allowed/blocked content types.
- Verify object existence and expected size before marking an attachment `UPLOADED`.
- Publish `attachment.uploaded` after upload completion.

## Quota

- Decide whether quota is reserved at attachment initiate, upload complete, or email import.
- Prevent quota races for concurrent uploads/imports from the same user.

## Testing

- Add controller tests for validation and response codes.
- Add Testcontainers integration tests for Postgres, MinIO, and Kafka.
- Add failure-path tests for object storage and Kafka publish failures.

## Security

- Add asynchronous antivirus scanning.
- Block attachment download until scan verdict is clean.
- Quarantine infected attachments.

