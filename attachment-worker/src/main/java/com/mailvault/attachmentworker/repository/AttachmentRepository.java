package com.mailvault.attachmentworker.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AttachmentRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public AttachmentRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<AttachmentRow> findUploaded(int limit) {
        String sql = """
                SELECT id, user_id, filename, object_key, size_bytes
                  FROM attachments
                 WHERE status = 'UPLOADED'
                 ORDER BY created_at
                 LIMIT :limit
                """;

        return jdbcTemplate.query(
                sql,
                new MapSqlParameterSource("limit", limit),
                (rs, rowNum) -> new AttachmentRow(
                        rs.getObject("id", UUID.class),
                        rs.getString("user_id"),
                        rs.getString("filename"),
                        rs.getString("object_key"),
                        rs.getLong("size_bytes")
                )
        );
    }

    public boolean markProcessing(UUID attachmentId) {
        String sql = """
                UPDATE attachments
                   SET status = 'PROCESSING'
                 WHERE id = :attachmentId
                   AND status = 'UPLOADED'
                """;

        int updated = jdbcTemplate.update(sql, new MapSqlParameterSource("attachmentId", attachmentId));
        return updated == 1;
    }

    public Optional<AttachmentBlobRow> findBlobBySha256(String sha256) {
        String sql = """
                SELECT id, sha256, object_key, size_bytes, ref_count
                  FROM attachment_blobs
                 WHERE sha256 = :sha256
                """;

        List<AttachmentBlobRow> rows = jdbcTemplate.query(
                sql,
                new MapSqlParameterSource("sha256", sha256),
                (rs, rowNum) -> new AttachmentBlobRow(
                        rs.getObject("id", UUID.class),
                        rs.getString("sha256"),
                        rs.getString("object_key"),
                        rs.getLong("size_bytes"),
                        rs.getLong("ref_count")
                )
        );
        return rows.stream().findFirst();
    }

    public boolean insertBlob(AttachmentBlobRow blob, Instant createdAt) {
        String sql = """
                INSERT INTO attachment_blobs (id, sha256, object_key, size_bytes, ref_count, created_at)
                VALUES (:id, :sha256, :objectKey, :sizeBytes, :refCount, :createdAt)
                """;

        try {
            jdbcTemplate.update(
                    sql,
                    new MapSqlParameterSource()
                            .addValue("id", blob.id())
                            .addValue("sha256", blob.sha256())
                            .addValue("objectKey", blob.objectKey())
                            .addValue("sizeBytes", blob.sizeBytes())
                            .addValue("refCount", blob.refCount())
                            .addValue("createdAt", OffsetDateTime.ofInstant(createdAt, ZoneOffset.UTC))
            );
            return true;
        } catch (DuplicateKeyException exception) {
            return false;
        }
    }

    public void incrementBlobRefCount(UUID blobId) {
        jdbcTemplate.update(
                "UPDATE attachment_blobs SET ref_count = ref_count + 1 WHERE id = :blobId",
                new MapSqlParameterSource("blobId", blobId)
        );
    }

    public void markReady(UUID attachmentId, String sha256, UUID blobId, String canonicalObjectKey) {
        String sql = """
                UPDATE attachments
                   SET status = 'READY',
                       sha256 = :sha256,
                       blob_id = :blobId,
                       object_key = :canonicalObjectKey
                 WHERE id = :attachmentId
                """;

        jdbcTemplate.update(
                sql,
                new MapSqlParameterSource()
                        .addValue("attachmentId", attachmentId)
                        .addValue("sha256", sha256)
                        .addValue("blobId", blobId)
                        .addValue("canonicalObjectKey", canonicalObjectKey)
        );
    }

    public void markFailed(UUID attachmentId) {
        jdbcTemplate.update(
                "UPDATE attachments SET status = 'FAILED' WHERE id = :attachmentId",
                new MapSqlParameterSource("attachmentId", attachmentId)
        );
    }
}
