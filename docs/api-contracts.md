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

Import starts a new MailVault thread. It does not try to merge the message into an existing conversation by subject or external mail headers.

## Create Draft

```http
POST /drafts
Content-Type: application/json
```

```json
{
  "userId": "user-123",
  "from": "abhinav@example.com",
  "to": ["billing@example.com"],
  "cc": [],
  "bcc": [],
  "subject": "Draft invoice reply",
  "textBody": "I will review this.",
  "htmlBody": null,
  "attachmentIds": []
}
```

```json
{
  "emailId": "7907d0dc-0a2b-4687-a717-e2d522c6789d",
  "status": "DRAFT",
  "logicalSizeBytes": 19
}
```

Draft creation stores the message body and metadata, creates a `DRAFT` mailbox thread, and links the draft through `thread_messages.direction = DRAFT`. It does not publish `email.received`.

## Send Draft

```http
POST /drafts/{emailId}/send
Content-Type: application/json
```

```json
{
  "userId": "user-123"
}
```

```json
{
  "emailId": "7907d0dc-0a2b-4687-a717-e2d522c6789d",
  "status": "ACCEPTED",
  "logicalSizeBytes": 19
}
```

Sending a draft transitions the existing message from `EmailStatus.DRAFT` to `INDEX_PENDING`, changes the linked `thread_messages.direction` from `DRAFT` to `OUTBOUND`, removes the thread from the `DRAFT` view, and publishes `email.received` for downstream quota and search processing.

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

Supported folders/views:

```text
INBOX - inbound mailbox threads
SENT  - threads containing outbound messages
DRAFT - unsent draft threads
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
    "attachmentCount": 1,
    "labels": [
      {
        "label": "IMPORTANT",
        "source": "USER",
        "confidenceScore": null
      }
    ]
  }
]
```

```http
GET /threads/{threadId}?userId=user-123
```

Thread detail returns the ordered messages in the conversation. Attachments and recipients remain message-level data.

## Thread Labels

Labels are per-user metadata on a thread. They do not replace folders: a thread can remain in `INBOX` while also being labeled `IMPORTANT`, `FAVORITE`, `PROMOTION`, or a custom label.

The current API supports user-applied labels. The schema also stores `source` and optional `confidenceScore` so later classifier-driven labels can be recorded as `SYSTEM` or `AI` without changing the core table shape.

```http
POST /threads/{threadId}/labels/{label}?userId=user-123
```

```json
{
  "threadId": "1f72f814-6e41-44c1-bb48-6ad24ab68a5b",
  "status": "LABEL_ADDED",
  "label": "IMPORTANT",
  "source": "USER",
  "labels": [
    {
      "label": "IMPORTANT",
      "source": "USER",
      "confidenceScore": null
    }
  ]
}
```

Labels are normalized to uppercase. Valid labels are 1-40 characters using letters, numbers, underscore, or hyphen.

```http
DELETE /threads/{threadId}/labels/{label}?userId=user-123
```

```json
{
  "threadId": "1f72f814-6e41-44c1-bb48-6ad24ab68a5b",
  "status": "LABEL_REMOVED",
  "label": "IMPORTANT",
  "source": "USER",
  "labels": []
}
```

Threads can be listed by label:

```http
GET /mailboxes/{userId}/labels/{label}/threads?limit=20
```

## Mark Thread Read

```http
POST /threads/{threadId}/read?userId=user-123
```

```json
{
  "threadId": "1f72f814-6e41-44c1-bb48-6ad24ab68a5b",
  "status": "READ",
  "unreadCount": 0,
  "folder": "INBOX"
}
```

Marks all unread `INBOUND` messages in the thread as read by setting `thread_messages.read_at`, then updates `mailbox_threads.unread_count`.

## Mark Thread Unread

```http
POST /threads/{threadId}/unread?userId=user-123
```

```json
{
  "threadId": "1f72f814-6e41-44c1-bb48-6ad24ab68a5b",
  "status": "UNREAD",
  "unreadCount": 1,
  "folder": "INBOX"
}
```

Marks the latest `INBOUND` message as unread by clearing its `read_at`. If the thread has no inbound messages, the response is `NO_INBOUND_MESSAGE` with `unreadCount = 0`.

## Mailbox Thread Actions

```http
POST /threads/{threadId}/archive?userId=user-123
POST /threads/{threadId}/trash?userId=user-123
POST /threads/{threadId}/spam?userId=user-123
POST /threads/{threadId}/restore?userId=user-123
```

Archive, trash, and spam update `mailbox_threads.folder` to `ARCHIVE`, `TRASH`, or `SPAM`. Restore moves the thread back to `INBOX` if it has inbound messages; otherwise it moves to `ACTIVE`.

```json
{
  "threadId": "1f72f814-6e41-44c1-bb48-6ad24ab68a5b",
  "status": "ARCHIVED",
  "unreadCount": 1,
  "folder": "ARCHIVE"
}
```

## Reply To Thread

```http
POST /threads/{threadId}/messages
Content-Type: application/json
```

```json
{
  "userId": "user-123",
  "from": "abhinav@example.com",
  "to": ["billing@example.com"],
  "cc": [],
  "bcc": [],
  "subject": "Re: Invoice for May",
  "textBody": "Thanks, received.",
  "htmlBody": "<p>Thanks, received.</p>",
  "attachmentIds": []
}
```

```json
{
  "emailId": "9ad60756-67c4-4914-a27f-35956ddba7f9",
  "status": "ACCEPTED",
  "logicalSizeBytes": 42
}
```

This API appends a new message to an existing thread after validating that the thread belongs to the user. External mailbox migration, if added later, should be a separate adapter that converts provider conversations into MailVault threads and messages before persistence.

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
