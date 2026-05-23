# Search Indexer

Consumes `email.received` events and builds the OpenSearch read model for MailVault.

## Responsibilities

- Consume email events from Kafka.
- Convert events into search documents.
- Upsert documents into OpenSearch by `emailId`.
- Keep search eventually consistent with the email metadata source of truth.

The current MVP indexes fields already present in `email.received`: user, sender, recipients, subject, logical size, and received timestamp. Body indexing should be added after the indexer can safely read normalized body content from object storage or a dedicated content projection.

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
