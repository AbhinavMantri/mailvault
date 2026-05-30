ALTER TABLE mailbox_thread_labels
    ADD COLUMN source VARCHAR(20) NOT NULL DEFAULT 'USER',
    ADD COLUMN confidence_score NUMERIC(5,4);

ALTER TABLE mailbox_thread_labels
    DROP CONSTRAINT mailbox_thread_labels_pkey;

ALTER TABLE mailbox_thread_labels
    ADD PRIMARY KEY (user_id, thread_id, label, source);

DROP INDEX idx_mailbox_thread_labels_user_label;

CREATE INDEX idx_mailbox_thread_labels_user_label
    ON mailbox_thread_labels (user_id, label, source, thread_id);
