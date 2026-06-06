CREATE TABLE conversations (
    id UUID PRIMARY KEY,
    subject_normalized VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

INSERT INTO conversations (id, subject_normalized, created_at, updated_at)
SELECT id, subject_normalized, created_at, updated_at
FROM mailbox_threads;

ALTER TABLE mailbox_threads
    ADD COLUMN conversation_id UUID;

UPDATE mailbox_threads
SET conversation_id = id;

ALTER TABLE mailbox_threads
    ALTER COLUMN conversation_id SET NOT NULL;

ALTER TABLE mailbox_threads
    ADD CONSTRAINT fk_mailbox_threads_conversation
        FOREIGN KEY (conversation_id) REFERENCES conversations (id);

CREATE INDEX idx_mailbox_threads_conversation
    ON mailbox_threads (conversation_id);
