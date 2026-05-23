CREATE TABLE emails (
    id UUID PRIMARY KEY,
    user_id VARCHAR(128) NOT NULL,
    sender VARCHAR(320) NOT NULL,
    subject VARCHAR(512) NOT NULL,
    raw_object_key VARCHAR(1024) NOT NULL,
    text_object_key VARCHAR(1024) NOT NULL,
    html_object_key VARCHAR(1024),
    logical_size_bytes BIGINT NOT NULL,
    status VARCHAR(40) NOT NULL,
    received_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_emails_user_received_at ON emails (user_id, received_at DESC);

CREATE TABLE email_recipients (
    id UUID PRIMARY KEY,
    email_id UUID NOT NULL REFERENCES emails(id) ON DELETE CASCADE,
    recipient_address VARCHAR(320) NOT NULL,
    recipient_type VARCHAR(20) NOT NULL
);

CREATE INDEX idx_email_recipients_email_id ON email_recipients (email_id);

CREATE TABLE attachments (
    id UUID PRIMARY KEY,
    user_id VARCHAR(128) NOT NULL,
    filename VARCHAR(512) NOT NULL,
    sha256 CHAR(64),
    object_key VARCHAR(1024) NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    size_bytes BIGINT NOT NULL,
    status VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX idx_attachments_sha256_ready ON attachments (sha256) WHERE sha256 IS NOT NULL;
CREATE INDEX idx_attachments_user_status ON attachments (user_id, status);

CREATE TABLE email_attachment_refs (
    id UUID PRIMARY KEY,
    email_id UUID NOT NULL REFERENCES emails(id) ON DELETE CASCADE,
    attachment_id UUID NOT NULL REFERENCES attachments(id)
);

CREATE INDEX idx_email_attachment_refs_email_id ON email_attachment_refs (email_id);
CREATE INDEX idx_email_attachment_refs_attachment_id ON email_attachment_refs (attachment_id);

CREATE TABLE storage_usage (
    user_id VARCHAR(128) PRIMARY KEY,
    used_bytes BIGINT NOT NULL,
    quota_bytes BIGINT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_outbox_events_published_created_at ON outbox_events (published, created_at);
