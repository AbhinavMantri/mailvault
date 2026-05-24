CREATE TABLE storage_usage_events (
    event_id UUID PRIMARY KEY,
    email_id UUID NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    bytes_delta BIGINT NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_storage_usage_events_user_processed_at
    ON storage_usage_events (user_id, processed_at);
