# Attachment Worker

Processes uploaded attachments after the upload API marks them `UPLOADED`.

## Responsibilities

- Poll uploaded attachment records.
- Claim attachments for processing.
- Read attachment bytes from MinIO.
- Compute SHA-256 content hash.
- Mark attachments `READY` after hash calculation.
- Mark attachments `FAILED` when processing cannot complete.

The current foundation stores the hash on the `attachments` row. True physical deduplication should later introduce a canonical `attachment_blobs` table with `sha256`, canonical object key, size, and reference count.

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
