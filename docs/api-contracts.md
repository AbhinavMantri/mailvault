# API Contracts

Initial API shape for the MVP.

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
  "attachments": [
    {
      "filename": "invoice.pdf",
      "contentType": "application/pdf",
      "base64Content": "..."
    }
  ]
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

## Storage Usage

```http
GET /users/{userId}/storage
```

```json
{
  "usedBytes": 1240000000,
  "quotaBytes": 5368709120,
  "usagePercent": 23.1
}
```

