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

`attachment-worker` later picks up `UPLOADED` rows, computes SHA-256 from object bytes, links each attachment to a canonical blob, deletes the temporary pending object, and marks each attachment `READY` or `FAILED`.

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

After storing the imported email, `quota-service` consumes `email.received` and updates logical storage usage asynchronously.


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

`search-service` owns the user-facing search API. The current implementation queries OpenSearch documents written by `search-indexer`.

```json
[
  {
    "emailId": "28c13478-95df-4c11-a7f5-3695f63202f7",
    "sender": "billing@example.com",
    "recipients": ["abhinav@example.com"],
    "subject": "Invoice for May",
    "receivedAt": "2026-05-23T12:00:00Z",
    "logicalSizeBytes": 2048
  }
]
```

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
