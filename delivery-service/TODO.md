# Delivery Service TODO

Hardening items before treating SMTP delivery as production-ready.

## Reliability

- Consume outbound delivery events from Kafka instead of relying on synchronous API calls from ingestion.
- Add retry policy with exponential backoff and a dead-letter topic for permanently failed deliveries.
- Store delivery attempts, SMTP response codes, and final delivery state durably.
- Add idempotency keys per outbound email and recipient to avoid duplicate sends.

## Email Operations

- Add bounce processing and suppression lists for invalid or repeatedly failing recipients.
- Add provider routing so production can use SES, SendGrid, Postfix, or another SMTP relay behind the same service contract.
- Add DKIM signing support before sending through a public SMTP relay.
- Add rate limits by sender, domain, and destination provider.

## Testing

- Keep GreenMail as the local SMTP test server for integration tests.
- Add controller-level tests for request validation and API response codes.
- Add a local E2E script that starts GreenMail and verifies a message reaches the test SMTP inbox.
