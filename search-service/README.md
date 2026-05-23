# Search Service

Serves user-facing email search queries from OpenSearch.

## Responsibilities

- Expose `GET /emails/search`.
- Query OpenSearch with user-scoped filters.
- Return bounded search results for mailbox clients.
- Keep query scaling separate from mailbox inbox/detail reads.

`search-indexer` writes OpenSearch documents asynchronously. This service reads those documents and owns query behavior, ranking, pagination, timeouts, and future search-specific rate limits.

## Run Locally

From the repository root:

```bash
docker compose up -d
```

Then start the service:

```bash
cd search-service
mvn spring-boot:run
```

Health check:

```bash
curl http://localhost:8085/actuator/health
```

Search:

```bash
curl "http://localhost:8085/emails/search?userId=user-123&q=invoice&limit=20"
```
