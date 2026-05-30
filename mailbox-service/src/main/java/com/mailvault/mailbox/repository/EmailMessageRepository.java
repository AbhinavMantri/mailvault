package com.mailvault.mailbox.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
                       COUNT(DISTINCT ear.id) AS attachment_count
                  FROM emails e
                  JOIN thread_messages tm ON tm.email_id = e.id
                                        AND tm.direction = 'INBOUND'
                  JOIN mailbox_threads mt ON mt.id = tm.thread_id
                                         AND mt.user_id = :userId
                                         AND mt.folder = 'INBOX'
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

    public List<ThreadSummaryRow> findThreads(String userId, String folder, int limit) {
        if ("SENT".equalsIgnoreCase(folder)) {
            return findSentThreads(userId, limit);
        }
        String sql = """
                SELECT ut.id,
                       latest.subject,
                       ut.folder,
                       ut.last_sender,
                       ut.last_message_at,
                       ut.message_count,
                       ut.unread_count,
                       COUNT(DISTINCT ear.id) AS attachment_count
                  FROM mailbox_threads ut
                  JOIN LATERAL (
                      SELECT e.subject
                        FROM thread_messages tm
                        JOIN emails e ON e.id = tm.email_id
                       WHERE tm.thread_id = ut.id
                       ORDER BY tm.created_at DESC
                       LIMIT 1
                  ) latest ON TRUE
                  LEFT JOIN thread_messages tm_all ON tm_all.thread_id = ut.id
                  LEFT JOIN email_attachment_refs ear ON ear.email_id = tm_all.email_id
                 WHERE ut.user_id = :userId
                   AND ut.folder = :folder
                 GROUP BY ut.id, latest.subject, ut.folder, ut.last_sender, ut.last_message_at,
                          ut.message_count, ut.unread_count
                 ORDER BY ut.last_message_at DESC
                 LIMIT :limit
                """;

        return jdbcTemplate.query(
                sql,
                new MapSqlParameterSource()
                        .addValue("userId", userId)
                        .addValue("folder", folder)
                        .addValue("limit", limit),
                (rs, rowNum) -> new ThreadSummaryRow(
                        rs.getObject("id", UUID.class),
                        rs.getString("subject"),
                        rs.getString("folder"),
                        rs.getString("last_sender"),
                        toInstant(rs, "last_message_at"),
                        rs.getInt("message_count"),
                        rs.getInt("unread_count"),
                        rs.getInt("attachment_count")
                )
        );
    }

    public List<ThreadSummaryRow> findThreadsByLabel(String userId, String label, int limit) {
        String sql = """
                SELECT ut.id,
                       latest.subject,
                       ut.folder,
                       ut.last_sender,
                       ut.last_message_at,
                       ut.message_count,
                       ut.unread_count,
                       COUNT(DISTINCT ear.id) AS attachment_count
                  FROM mailbox_thread_labels mtl
                  JOIN mailbox_threads ut ON ut.id = mtl.thread_id
                                         AND ut.user_id = mtl.user_id
                  JOIN LATERAL (
                      SELECT e.subject
                        FROM thread_messages tm
                        JOIN emails e ON e.id = tm.email_id
                       WHERE tm.thread_id = ut.id
                       ORDER BY tm.created_at DESC
                       LIMIT 1
                  ) latest ON TRUE
                  LEFT JOIN thread_messages tm_all ON tm_all.thread_id = ut.id
                  LEFT JOIN email_attachment_refs ear ON ear.email_id = tm_all.email_id
                 WHERE mtl.user_id = :userId
                   AND mtl.label = :label
                 GROUP BY ut.id, latest.subject, ut.folder, ut.last_sender, ut.last_message_at,
                          ut.message_count, ut.unread_count
                 ORDER BY ut.last_message_at DESC
                 LIMIT :limit
                """;

        return jdbcTemplate.query(
                sql,
                new MapSqlParameterSource()
                        .addValue("userId", userId)
                        .addValue("label", label)
                        .addValue("limit", limit),
                (rs, rowNum) -> new ThreadSummaryRow(
                        rs.getObject("id", UUID.class),
                        rs.getString("subject"),
                        rs.getString("folder"),
                        rs.getString("last_sender"),
                        toInstant(rs, "last_message_at"),
                        rs.getInt("message_count"),
                        rs.getInt("unread_count"),
                        rs.getInt("attachment_count")
                )
        );
    }

    private List<ThreadSummaryRow> findSentThreads(String userId, int limit) {
        String sql = """
                SELECT ut.id,
                       latest.subject,
                       'SENT' AS folder,
                       ut.last_sender,
                       ut.last_message_at,
                       ut.message_count,
                       ut.unread_count,
                       COUNT(DISTINCT ear.id) AS attachment_count
                  FROM mailbox_threads ut
                  JOIN LATERAL (
                      SELECT e.subject
                        FROM thread_messages tm
                        JOIN emails e ON e.id = tm.email_id
                       WHERE tm.thread_id = ut.id
                       ORDER BY tm.created_at DESC
                       LIMIT 1
                  ) latest ON TRUE
                 JOIN thread_messages sent_tm ON sent_tm.thread_id = ut.id
                                              AND sent_tm.direction = 'OUTBOUND'
                  LEFT JOIN thread_messages tm_all ON tm_all.thread_id = ut.id
                  LEFT JOIN email_attachment_refs ear ON ear.email_id = tm_all.email_id
                 WHERE ut.user_id = :userId
                   AND ut.folder NOT IN ('TRASH', 'SPAM')
                 GROUP BY ut.id, latest.subject, ut.last_sender, ut.last_message_at,
                          ut.message_count, ut.unread_count
                 ORDER BY MAX(sent_tm.created_at) DESC
                 LIMIT :limit
                """;

        return jdbcTemplate.query(
                sql,
                new MapSqlParameterSource()
                        .addValue("userId", userId)
                        .addValue("limit", limit),
                (rs, rowNum) -> new ThreadSummaryRow(
                        rs.getObject("id", UUID.class),
                        rs.getString("subject"),
                        rs.getString("folder"),
                        rs.getString("last_sender"),
                        toInstant(rs, "last_message_at"),
                        rs.getInt("message_count"),
                        rs.getInt("unread_count"),
                        rs.getInt("attachment_count")
                )
        );
    }

    public Optional<ThreadSummaryRow> findThread(UUID threadId, String userId) {
        String sql = """
                SELECT ut.id,
                       latest.subject,
                       ut.folder,
                       ut.last_sender,
                       ut.last_message_at,
                       ut.message_count,
                       ut.unread_count,
                       COUNT(ear.id) AS attachment_count
                  FROM mailbox_threads ut
                  JOIN LATERAL (
                      SELECT e.subject
                        FROM thread_messages tm
                        JOIN emails e ON e.id = tm.email_id
                       WHERE tm.thread_id = ut.id
                       ORDER BY tm.created_at DESC
                       LIMIT 1
                  ) latest ON TRUE
                  LEFT JOIN thread_messages tm_all ON tm_all.thread_id = ut.id
                  LEFT JOIN email_attachment_refs ear ON ear.email_id = tm_all.email_id
                 WHERE ut.id = :threadId
                   AND ut.user_id = :userId
                 GROUP BY ut.id, latest.subject, ut.folder, ut.last_sender, ut.last_message_at,
                          ut.message_count, ut.unread_count
                """;

        List<ThreadSummaryRow> rows = jdbcTemplate.query(
                sql,
                new MapSqlParameterSource()
                        .addValue("threadId", threadId)
                        .addValue("userId", userId),
                (rs, rowNum) -> new ThreadSummaryRow(
                        rs.getObject("id", UUID.class),
                        rs.getString("subject"),
                        rs.getString("folder"),
                        rs.getString("last_sender"),
                        toInstant(rs, "last_message_at"),
                        rs.getInt("message_count"),
                        rs.getInt("unread_count"),
                        rs.getInt("attachment_count")
                )
        );
        return rows.stream().findFirst();
    }

    public boolean threadExists(UUID threadId, String userId) {
        String sql = """
                SELECT COUNT(*)
                  FROM mailbox_threads
                 WHERE id = :threadId
                   AND user_id = :userId
                """;

        Integer count = jdbcTemplate.queryForObject(
                sql,
                new MapSqlParameterSource()
                        .addValue("threadId", threadId)
                        .addValue("userId", userId),
                Integer.class
        );
        return count != null && count > 0;
    }

    public String findThreadFolder(UUID threadId, String userId) {
        String sql = """
                SELECT folder
                  FROM mailbox_threads
                 WHERE id = :threadId
                   AND user_id = :userId
                """;
        return jdbcTemplate.queryForObject(
                sql,
                new MapSqlParameterSource()
                        .addValue("threadId", threadId)
                        .addValue("userId", userId),
                String.class
        );
    }

    public int markInboundMessagesRead(UUID threadId) {
        String sql = """
                UPDATE thread_messages
                   SET read_at = now()
                 WHERE thread_id = :threadId
                   AND direction = 'INBOUND'
                   AND read_at IS NULL
                """;
        return jdbcTemplate.update(sql, new MapSqlParameterSource("threadId", threadId));
    }

    public boolean markLatestInboundMessageUnread(UUID threadId) {
        String sql = """
                UPDATE thread_messages
                   SET read_at = NULL
                 WHERE id = (
                       SELECT id
                         FROM thread_messages
                        WHERE thread_id = :threadId
                          AND direction = 'INBOUND'
                        ORDER BY created_at DESC
                        LIMIT 1
                 )
                """;
        return jdbcTemplate.update(sql, new MapSqlParameterSource("threadId", threadId)) > 0;
    }

    public int updateThreadUnreadCount(UUID threadId, String userId, int unreadCount) {
        String sql = """
                UPDATE mailbox_threads
                   SET unread_count = :unreadCount,
                       updated_at = now()
                 WHERE id = :threadId
                   AND user_id = :userId
                """;
        return jdbcTemplate.update(
                sql,
                new MapSqlParameterSource()
                        .addValue("threadId", threadId)
                        .addValue("userId", userId)
                        .addValue("unreadCount", unreadCount)
        );
    }

    public int updateThreadFolder(UUID threadId, String userId, String folder) {
        String sql = """
                UPDATE mailbox_threads
                   SET folder = :folder,
                       updated_at = now()
                 WHERE id = :threadId
                   AND user_id = :userId
                """;
        return jdbcTemplate.update(
                sql,
                new MapSqlParameterSource()
                        .addValue("threadId", threadId)
                        .addValue("userId", userId)
                        .addValue("folder", folder)
        );
    }

    public int addThreadLabel(UUID threadId, String userId, String label) {
        String sql = """
                INSERT INTO mailbox_thread_labels (user_id, thread_id, label, source, confidence_score, created_at)
                VALUES (:userId, :threadId, :label, 'USER', NULL, now())
                ON CONFLICT (user_id, thread_id, label, source) DO NOTHING
                """;

        return jdbcTemplate.update(
                sql,
                new MapSqlParameterSource()
                        .addValue("threadId", threadId)
                        .addValue("userId", userId)
                        .addValue("label", label)
        );
    }

    public int removeThreadLabel(UUID threadId, String userId, String label) {
        String sql = """
                DELETE FROM mailbox_thread_labels
                 WHERE thread_id = :threadId
                   AND user_id = :userId
                   AND label = :label
                   AND source = 'USER'
                """;

        return jdbcTemplate.update(
                sql,
                new MapSqlParameterSource()
                        .addValue("threadId", threadId)
                        .addValue("userId", userId)
                        .addValue("label", label)
        );
    }

    public List<ThreadLabelRow> findThreadLabels(UUID threadId, String userId) {
        String sql = """
                SELECT label,
                       source,
                       confidence_score
                  FROM mailbox_thread_labels
                 WHERE thread_id = :threadId
                   AND user_id = :userId
                 ORDER BY label, source
                """;

        return jdbcTemplate.query(
                sql,
                new MapSqlParameterSource()
                        .addValue("threadId", threadId)
                        .addValue("userId", userId),
                (rs, rowNum) -> new ThreadLabelRow(
                        rs.getString("label"),
                        rs.getString("source"),
                        rs.getBigDecimal("confidence_score")
                )
        );
    }

    public String restoreThread(UUID threadId, String userId) {
        String sql = """
                UPDATE mailbox_threads
                   SET folder = CASE
                                WHEN EXISTS (
                                    SELECT 1
                                      FROM thread_messages
                                     WHERE thread_id = :threadId
                                       AND direction = 'INBOUND'
                                )
                                THEN 'INBOX'
                                ELSE 'ACTIVE'
                                END,
                       updated_at = now()
                 WHERE id = :threadId
                   AND user_id = :userId
             RETURNING folder
                """;
        return jdbcTemplate.queryForObject(
                sql,
                new MapSqlParameterSource()
                        .addValue("threadId", threadId)
                        .addValue("userId", userId),
                String.class
        );
    }

    public int recalculateUnreadCount(UUID threadId) {
        String sql = """
                SELECT COUNT(*)
                  FROM thread_messages
                 WHERE thread_id = :threadId
                   AND direction = 'INBOUND'
                   AND read_at IS NULL
                """;
        Integer count = jdbcTemplate.queryForObject(
                sql,
                new MapSqlParameterSource("threadId", threadId),
                Integer.class
        );
        return count == null ? 0 : count;
    }

    public Map<UUID, List<ThreadLabelRow>> findLabelsForThreads(List<UUID> threadIds, String userId) {
        if (threadIds.isEmpty()) {
            return Collections.emptyMap();
        }

        String sql = """
                SELECT thread_id,
                       label,
                       source,
                       confidence_score
                  FROM mailbox_thread_labels
                 WHERE user_id = :userId
                   AND thread_id IN (:threadIds)
                 ORDER BY thread_id, label, source
                """;

        Map<UUID, List<ThreadLabelRow>> labelsByThread = new HashMap<>();
        jdbcTemplate.query(
                sql,
                new MapSqlParameterSource()
                        .addValue("userId", userId)
                        .addValue("threadIds", threadIds),
                rs -> {
                    UUID threadId = rs.getObject("thread_id", UUID.class);
                    labelsByThread.computeIfAbsent(threadId, ignored -> new java.util.ArrayList<>())
                            .add(new ThreadLabelRow(
                                    rs.getString("label"),
                                    rs.getString("source"),
                                    rs.getBigDecimal("confidence_score")
                            ));
                }
        );
        return labelsByThread;
    }

    public List<ThreadMessageRow> findThreadMessages(UUID threadId) {
        String sql = """
                SELECT tm.thread_id,
                       e.id AS email_id,
                       tm.direction,
                       e.sender,
                       e.subject,
                       e.text_object_key,
                       e.html_object_key,
                       e.received_at,
                       e.logical_size_bytes
                  FROM thread_messages tm
                  JOIN emails e ON e.id = tm.email_id
                 WHERE tm.thread_id = :threadId
                 ORDER BY tm.created_at
                """;

        return jdbcTemplate.query(
                sql,
                new MapSqlParameterSource("threadId", threadId),
                (rs, rowNum) -> new ThreadMessageRow(
                        rs.getObject("thread_id", UUID.class),
                        rs.getObject("email_id", UUID.class),
                        rs.getString("direction"),
                        rs.getString("sender"),
                        rs.getString("subject"),
                        rs.getString("text_object_key"),
                        rs.getString("html_object_key"),
                        toInstant(rs, "received_at"),
                        rs.getLong("logical_size_bytes")
                )
        );
    }

    public Optional<EmailHeaderRow> findHeader(UUID emailId, String userId) {
        String sql = """
                SELECT id,
                       user_id,
                       sender,
                       subject,
                       text_object_key,
                       html_object_key,
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
                        rs.getString("text_object_key"),
                        rs.getString("html_object_key"),
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
                 ORDER BY CASE recipient_type
                            WHEN 'TO' THEN 1
                            WHEN 'CC' THEN 2
                            WHEN 'BCC' THEN 3
                            ELSE 4
                          END,
                          recipient_address
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
