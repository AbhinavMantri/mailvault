# Attachment Worker TODO

Hardening items before treating `attachment-worker` as production-ready.

## Deduplication

- Add `attachment_blobs` table for canonical physical objects.
- Move duplicate attachments to reference canonical blob rows.
- Decide how physical dedupe interacts with logical quota accounting.

## Reliability

- Replace polling with `attachment.uploaded` events after the outbox is complete.
- Add retry/backoff and max-attempt handling for transient MinIO failures.
- Add dead-letter handling for permanently failed attachments.
- Verify object size against the expected `size_bytes` before marking `READY`.

## Security

- Integrate with `attachment-scanner` before allowing downloads.
- Keep `READY` separate from `CLEAN` scan verdict when antivirus scanning is added.
