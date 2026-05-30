CREATE TABLE mailbox_thread_labels (
    user_id VARCHAR(128) NOT NULL,
    thread_id UUID NOT NULL REFERENCES mailbox_threads(id) ON DELETE CASCADE,
    label VARCHAR(80) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (user_id, thread_id, label)
);

CREATE INDEX idx_mailbox_thread_labels_user_label
    ON mailbox_thread_labels (user_id, label, thread_id);
