# Mailbox Service TODO

Hardening and product items before treating `mailbox-service` as production-ready.

## Mailbox Actions

- Add archive, trash, spam, and restore APIs once the mailbox-actions branch is merged.
- Add bulk actions for selected thread IDs.
- Add pagination cursors for thread list APIs instead of limit-only reads.

## Labels And Classification

- Keep manual labels as `source = USER` and never let classifier updates overwrite them.
- Add asynchronous system/AI classification for labels such as `PROMOTION`, `SOCIAL`, `SECURITY_ALERT`, and `SPAM_SUSPECTED`.
- Add classifier confidence handling and user override behavior for `source = AI` labels.
- Add label search/filter support in `search-service` and `search-indexer`.

## Forwarding

- Add optional mailbox UX support for forwarding into an existing thread if product requirements need it.
- Add UI-facing metadata for forwarded thread messages if clients need to show that a sent message was generated from a full conversation.
- Add explicit attachment selection visibility for forwarded messages.

## Testing

- Add controller tests for mailbox action and label validation errors.
- Add repository integration tests for thread folders, read state, label filtering, and canonical recipient visibility.
