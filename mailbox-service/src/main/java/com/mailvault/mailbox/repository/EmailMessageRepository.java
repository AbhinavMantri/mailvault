package com.mailvault.mailbox.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class EmailMessageRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public EmailMessageRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<InboxRow> findInbox(String userId, int limit) {
        String sql = """
                SELECT e.id,
                       e.sender,
                       e.subject,
                       e.status,
                       e.received_at,
                       e.logical_size_bytes,
                       COUNT(ear.id) AS attachment_count
                  FROM emails e
                  LEFT JOIN email_attachment_refs ear ON ear.email_id = e.id
                 WHERE e.user_id = :userId
                 GROUP BY e.id, e.sender, e.subject, e.status, e.received_at, e.logical_size_bytes
                 ORDER BY e.received_at DESC
                 LIMIT :limit
                """;

        return jdbcTemplate.query(
                sql,
                new MapSqlParameterSource()
                        .addValue("userId", userId)
                        .addValue("limit", limit),
                (rs, rowNum) -> new InboxRow(
                        rs.getObject("id", UUID.class),
                        rs.getString("sender"),
                        rs.getString("subject"),
                        rs.getString("status"),
                        toInstant(rs, "received_at"),
                        rs.getLong("logical_size_bytes"),
                        rs.getInt("attachment_count")
                )
        );
    }

    public Optional<EmailHeaderRow> findHeader(UUID emailId, String userId) {
        String sql = """
                SELECT id,
                       user_id,
                       sender,
                       subject,
                       status,
                       received_at,
                       logical_size_bytes
                  FROM emails
                 WHERE id = :emailId
                   AND user_id = :userId
                """;

        List<EmailHeaderRow> rows = jdbcTemplate.query(
                sql,
                new MapSqlParameterSource()
                        .addValue("emailId", emailId)
                        .addValue("userId", userId),
                (rs, rowNum) -> new EmailHeaderRow(
                        rs.getObject("id", UUID.class),
                        rs.getString("user_id"),
                        rs.getString("sender"),
                        rs.getString("subject"),
                        rs.getString("status"),
                        toInstant(rs, "received_at"),
                        rs.getLong("logical_size_bytes")
                )
        );
        return rows.stream().findFirst();
    }

    public List<RecipientRow> findRecipients(UUID emailId) {
        String sql = """
                SELECT recipient_address,
                       recipient_type
                  FROM email_recipients
                 WHERE email_id = :emailId
                 ORDER BY recipient_type, recipient_address
                """;

        return jdbcTemplate.query(
                sql,
                new MapSqlParameterSource("emailId", emailId),
                (rs, rowNum) -> new RecipientRow(
                        rs.getString("recipient_address"),
                        rs.getString("recipient_type")
                )
        );
    }

    public List<AttachmentRow> findAttachments(UUID emailId) {
        String sql = """
                SELECT a.id,
                       a.filename,
                       a.content_type,
                       a.size_bytes,
                       a.status
                  FROM email_attachment_refs ear
                  JOIN attachments a ON a.id = ear.attachment_id
                 WHERE ear.email_id = :emailId
                 ORDER BY a.filename, a.id
                """;

        return jdbcTemplate.query(
                sql,
                new MapSqlParameterSource("emailId", emailId),
                (rs, rowNum) -> new AttachmentRow(
                        rs.getObject("id", UUID.class),
                        rs.getString("filename"),
                        rs.getString("content_type"),
                        rs.getLong("size_bytes"),
                        rs.getString("status")
                )
        );
    }

    private Instant toInstant(ResultSet rs, String columnName) throws SQLException {
        return rs.getObject(columnName, OffsetDateTime.class).toInstant();
    }
}
