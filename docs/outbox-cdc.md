# Outbox and CDC Strategy

MailVault uses the transactional outbox pattern so email persistence and event intent commit together.

## Current MVP

```text
ingestion-service
  -> saves email metadata
  -> saves email.received row in outbox_events

scheduled outbox publisher
  -> reads unpublished rows
  -> publishes to Kafka
  -> marks rows published after Kafka accepts the send
```

This keeps the request path reliable without requiring Kafka to be available during the database transaction.

## Kubernetes Production Direction

For a Kubernetes production deployment, the preferred direction is CDC-based outbox publishing with Debezium and Kafka Connect.

```text
ingestion-service
  -> writes emails and outbox_events to Postgres

Debezium Postgres connector
  -> reads committed outbox_events changes from the Postgres WAL
  -> publishes email.received to Kafka

search-indexer / archival-worker / quota projections
  -> consume Kafka events
```

This removes polling from application pods and avoids multi-node scheduler coordination inside `ingestion-service`.

## Why CDC Later

- API pods stay focused on user traffic.
- Event publication follows the database commit log.
- Multiple ingestion replicas do not compete to publish the same outbox rows.
- Kafka Connect/Debezium owns connector offsets, retries, and operational visibility.
- Consumers still remain idempotent because duplicate delivery is possible in distributed systems.

## Tradeoff

CDC adds infrastructure: Kafka Connect, Debezium connector configuration, schema discipline, and operational monitoring. For the MVP, the scheduled publisher keeps the project runnable locally. For production, CDC is the cleaner boundary.
