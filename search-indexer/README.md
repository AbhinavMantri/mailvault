# Search Indexer

Consumes `email.received` events and builds the OpenSearch read model for MailVault.

## Responsibilities

- Consume email events from Kafka.
- Skip recent duplicate events using Redis TTL idempotency keys.
- Convert events into search documents.
- Upsert documents into OpenSearch by `userId:emailId`.
- Keep search eventually consistent with the email metadata source of truth.

The current MVP indexes fields already present in `email.received`: user, sender, recipients, subject, logical size, and received timestamp. Body indexing should be added after the indexer can safely read normalized body content from object storage or a dedicated content projection.

Redis is only a duplicate-noise filter. If the Redis key is missing or Redis is unavailable, the durable fallback is still safe because OpenSearch uses `userId:emailId` as the document ID and upserts the same mailbox-visible document.

Canonical email rows can be visible in more than one mailbox after local delivery. The index identity is already scoped for that model, but recipient search fan-out still needs a mailbox visibility event or projection so the indexer can create one document per visible mailbox view.

## Run Locally

From the repository root:

```bash
docker compose up -d
```

Then start the service:

```bash
cd search-indexer
mvn spring-boot:run
```

Health check:

```bash
curl http://localhost:8084/actuator/health
```
