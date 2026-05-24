DROP INDEX IF EXISTS idx_attachments_sha256_ready;

CREATE TABLE attachment_blobs (
    id UUID PRIMARY KEY,
    sha256 CHAR(64) NOT NULL UNIQUE,
    object_key VARCHAR(1024) NOT NULL UNIQUE,
    size_bytes BIGINT NOT NULL,
    ref_count BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

ALTER TABLE attachments
    ADD COLUMN blob_id UUID REFERENCES attachment_blobs(id);

CREATE INDEX idx_attachments_blob_id ON attachments (blob_id);
