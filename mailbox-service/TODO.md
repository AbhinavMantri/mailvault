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

- Add email forwarding support after thread labels and mailbox actions are merged.
- Decide whether forwarded messages always start a new thread or can optionally append to an existing thread.
- Decide attachment policy for forwards: reference existing canonical attachments, copy logical attachment refs, or require explicit attachment selection.
- Ensure forwarding updates `SENT`, quota accounting, and search indexing consistently.

## Testing

- Add controller tests for mailbox action and label validation errors.
- Add repository integration tests for thread folders, read state, and label filtering.
