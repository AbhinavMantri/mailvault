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

## Message Composition

- Add optional forwarding into an existing thread if product requirements need it; the MVP starts forwards as new sent threads.
- Add explicit original-attachment selection for forwards instead of the current all-or-none `includeOriginalAttachments` flag.
- Add full-thread forwarding as a separate `POST /threads/{threadId}/forward` style feature that includes ordered conversation history.
- Add controller-level tests for forwarding request validation.

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
