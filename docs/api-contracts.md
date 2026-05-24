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

Completing the upload writes an `attachment.uploaded` outbox event. `attachment-worker` consumes the event, computes SHA-256 from object bytes, links each attachment to a canonical blob, deletes the temporary pending object, and marks each attachment `READY` or `FAILED`.

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
  "cc": ["manager@example.com"],
  "bcc": ["audit@example.com"],
  "subject": "Invoice for May",
  "textBody": "Invoice attached.",
  "htmlBody": "<p>Invoice attached.</p>",
  "attachmentIds": ["8e8f5f5c-5d4a-42b0-a3a4-7f1f8c0d9c99"]
}
```

After storing the imported email, `quota-service` consumes `email.received` and updates logical storage usage asynchronously.

Email import accepts attachment IDs that are already uploaded and not failed: `UPLOADED`, `PROCESSING`, or `READY`. This avoids a race where the Kafka-driven attachment worker marks an upload `READY` before the user sends the email.

## Get Email

```http
GET /emails/{emailId}
```

```json
{
  "id": "28c13478-95df-4c11-a7f5-3695f63202f7",
  "userId": "user-123",
  "sender": "billing@example.com",
  "subject": "Invoice for May",
  "textBody": "Invoice attached.",
  "htmlBody": "<p>Invoice attached.</p>",
  "status": "INDEX_PENDING",
  "logicalSizeBytes": 2048,
  "recipients": [
    {
      "address": "abhinav@example.com",
      "type": "TO"
    }
  ],
  "attachments": []
}
```

## Inbox

```http
GET /mailboxes/{userId}/inbox
```

## Threads

```http
GET /mailboxes/{userId}/threads?folder=INBOX
```

```json
[
  {
    "id": "1f72f814-6e41-44c1-bb48-6ad24ab68a5b",
    "subject": "Invoice for May",
    "folder": "INBOX",
    "lastSender": "billing@example.com",
    "lastMessageAt": "2026-05-23T12:00:00Z",
    "messageCount": 1,
    "unreadCount": 1,
    "attachmentCount": 1
  }
]
```

```http
GET /threads/{threadId}?userId=user-123
```

Thread detail returns the ordered messages in the conversation. Attachments and recipients remain message-level data.

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

## Report Email Abuse

Planned post-MVP API for user-triggered abuse, spam, phishing, or impersonation reports.

```http
POST /emails/{emailId}/report
Content-Type: application/json
```

```json
{
  "userId": "user-123",
  "reason": "ABUSE",
  "description": "Message contains threatening language"
}
```

Suggested reasons:

```text
SPAM
PHISHING
ABUSE
HARASSMENT
IMPERSONATION
OTHER
```

The API should record a durable report, apply a per-user `REPORTED` or `SPAM` label, and feed a moderation workflow. It should not delete the underlying email immediately because moderation actions need auditability and repeated reports may influence sender/domain risk scoring.
