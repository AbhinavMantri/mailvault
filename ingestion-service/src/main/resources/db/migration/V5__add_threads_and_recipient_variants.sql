ALTER TABLE email_recipients
    ADD COLUMN recipient_user_id VARCHAR(128),
    ADD COLUMN delivery_status VARCHAR(40) NOT NULL DEFAULT 'DELIVERED';

CREATE TABLE user_threads (
    id UUID PRIMARY KEY,
    user_id VARCHAR(128) NOT NULL,
    subject_normalized VARCHAR(512) NOT NULL,
    folder VARCHAR(40) NOT NULL,
    last_message_at TIMESTAMPTZ NOT NULL,
    last_sender VARCHAR(320) NOT NULL,
    message_count INTEGER NOT NULL,
    unread_count INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_user_threads_user_folder_last_message
    ON user_threads (user_id, folder, last_message_at DESC);

CREATE TABLE thread_messages (
    id UUID PRIMARY KEY,
    thread_id UUID NOT NULL REFERENCES user_threads(id) ON DELETE CASCADE,
    email_id UUID NOT NULL REFERENCES emails(id) ON DELETE CASCADE,
    direction VARCHAR(40) NOT NULL,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (thread_id, email_id)
);

CREATE INDEX idx_thread_messages_thread_created
    ON thread_messages (thread_id, created_at);

CREATE INDEX idx_thread_messages_email_id
    ON thread_messages (email_id);
