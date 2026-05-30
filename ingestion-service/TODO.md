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

- Add forwarding support after mailbox labels and mailbox actions are merged.
- Define whether forwarding starts a new thread by default or can append to an existing thread.
- Define how forwarded attachments are represented: reuse canonical blobs through new logical refs, copy selected attachment refs, or require explicit attachment selection.
- Publish the correct outbound event so quota and search update consistently.

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
