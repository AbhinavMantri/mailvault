package com.mailvault.attachmentworker.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
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

    public void markReady(UUID attachmentId, String sha256) {
        String sql = """
                UPDATE attachments
                   SET status = 'READY',
                       sha256 = :sha256
                 WHERE id = :attachmentId
                """;

        jdbcTemplate.update(
                sql,
                new MapSqlParameterSource()
                        .addValue("attachmentId", attachmentId)
                        .addValue("sha256", sha256)
        );
    }

    public void markFailed(UUID attachmentId) {
        jdbcTemplate.update(
                "UPDATE attachments SET status = 'FAILED' WHERE id = :attachmentId",
                new MapSqlParameterSource("attachmentId", attachmentId)
        );
    }
}
