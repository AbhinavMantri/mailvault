# API Contracts

Initial API shape for the MVP.

## Initiate Attachment Upload

```http
POST /attachments/initiate
Content-Type: application/json
```

```json
{
  "userId": "user-123",
  "filename": "invoice.pdf",
  "contentType": "application/pdf",
  "sizeBytes": 5242880
}
```

```json
{
  "attachmentId": "8e8f5f5c-5d4a-42b0-a3a4-7f1f8c0d9c99",
  "uploadUrl": "http://localhost:9000/...",
  "objectKey": "users/user-123/pending-attachments/8e8f5f5c-5d4a-42b0-a3a4-7f1f8c0d9c99/invoice.pdf",
  "expiresInSeconds": 900,
  "status": "PENDING_UPLOAD"
}
```

## Complete Attachment Upload

```http
POST /attachments/{attachmentId}/complete
```

```json
{
  "attachmentId": "8e8f5f5c-5d4a-42b0-a3a4-7f1f8c0d9c99",
  "status": "UPLOADED"
}
```

## Import Email

```http
POST /emails/import
Content-Type: application/json
```

```json
{
  "userId": "user-123",
  "from": "billing@example.com",
  "to": ["abhinav@example.com"],
  "subject": "Invoice for May",
  "textBody": "Invoice attached.",
  "htmlBody": "<p>Invoice attached.</p>",
  "attachmentIds": ["8e8f5f5c-5d4a-42b0-a3a4-7f1f8c0d9c99"]
}
```

Before storing the imported email, `ingestion-service` reserves logical bytes through `quota-service`.

## Reserve Quota

```http
POST /quota/reservations
Content-Type: application/json
```

```json
{
  "userId": "user-123",
  "bytes": 5242880
}
```

```json
{
  "userId": "user-123",
  "reservedBytes": 5242880,
  "usedBytes": 1240000000,
  "quotaBytes": 5368709120,
  "status": "RESERVED"
}
```

## Get Email

```http
GET /emails/{emailId}
```

## Inbox

```http
GET /mailboxes/{userId}/inbox
```

## Search

```http
GET /emails/search?userId=user-123&q=invoice
```

Search API is planned. The current implemented search path is the asynchronous `search-indexer`, which consumes `email.received` and upserts email documents into OpenSearch.

## Storage Usage

```http
GET /users/{userId}/storage
```

```json
{
  "userId": "user-123",
  "usedBytes": 1240000000,
  "quotaBytes": 5368709120,
  "usedPercent": 23.1,
  "updatedAt": "2026-05-23T15:00:00Z"
}
```
