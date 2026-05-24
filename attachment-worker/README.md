# Attachment Worker

Processes uploaded attachments after `ingestion-service` publishes `attachment.uploaded`.

## Responsibilities

- Consume `attachment.uploaded` events from Kafka.
- Claim attachments for processing.
- Read attachment bytes from MinIO.
- Compute SHA-256 content hash.
- Copy first-seen content into the canonical SHA-256 blob path.
- Reuse existing blob rows for duplicate content.
- Mark attachments `READY` after hash calculation.
- Mark attachments `FAILED` when processing cannot complete.

The current foundation stores the hash on the `attachments` row and links each attachment to an `attachment_blobs` row. Pending upload objects are deleted after the attachment points at the canonical blob object.

Duplicate events are safe: the worker claims work with the database transition `UPLOADED -> PROCESSING`, so already processed attachments are skipped.

## Run Locally

From the repository root:

```bash
docker compose up -d
```

Then start the worker:

```bash
cd attachment-worker
mvn spring-boot:run
```

Health check:

```bash
curl http://localhost:8086/actuator/health
```
