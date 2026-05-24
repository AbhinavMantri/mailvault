package com.mailvault.quota.repository;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class StorageUsageRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public StorageUsageRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<StorageUsageRow> findByUserId(String userId) {
        String sql = """
                SELECT user_id,
                       used_bytes,
                       quota_bytes,
                       updated_at
                  FROM storage_usage
                 WHERE user_id = :userId
                """;

        List<StorageUsageRow> rows = jdbcTemplate.query(
                sql,
                new MapSqlParameterSource("userId", userId),
                (rs, rowNum) -> new StorageUsageRow(
                        rs.getString("user_id"),
                        rs.getLong("used_bytes"),
                        rs.getLong("quota_bytes"),
                        toInstant(rs, "updated_at")
                )
        );
        return rows.stream().findFirst();
    }

    public Optional<StorageUsageRow> findByUserIdForUpdate(String userId) {
        String sql = """
                SELECT user_id,
                       used_bytes,
                       quota_bytes,
                       updated_at
                  FROM storage_usage
                 WHERE user_id = :userId
                   FOR UPDATE
                """;

        List<StorageUsageRow> rows = jdbcTemplate.query(
                sql,
                new MapSqlParameterSource("userId", userId),
                (rs, rowNum) -> new StorageUsageRow(
                        rs.getString("user_id"),
                        rs.getLong("used_bytes"),
                        rs.getLong("quota_bytes"),
                        toInstant(rs, "updated_at")
                )
        );
        return rows.stream().findFirst();
    }

    public StorageUsageRow insertDefaultUsage(String userId, long quotaBytes, Instant updatedAt) {
        String sql = """
                INSERT INTO storage_usage (user_id, used_bytes, quota_bytes, updated_at)
                VALUES (:userId, 0, :quotaBytes, :updatedAt)
                ON CONFLICT (user_id) DO NOTHING
                """;

        jdbcTemplate.update(
                sql,
                new MapSqlParameterSource()
                        .addValue("userId", userId)
                        .addValue("quotaBytes", quotaBytes)
                        .addValue("updatedAt", OffsetDateTime.ofInstant(updatedAt, java.time.ZoneOffset.UTC))
        );
        return findByUserIdForUpdate(userId)
                .orElse(new StorageUsageRow(userId, 0, quotaBytes, updatedAt));
    }

    public void updateUsage(String userId, long usedBytes, Instant updatedAt) {
        String sql = """
                UPDATE storage_usage
                   SET used_bytes = :usedBytes,
                       updated_at = :updatedAt
                 WHERE user_id = :userId
                """;

        jdbcTemplate.update(
                sql,
                new MapSqlParameterSource()
                        .addValue("userId", userId)
                        .addValue("usedBytes", usedBytes)
                        .addValue("updatedAt", OffsetDateTime.ofInstant(updatedAt, java.time.ZoneOffset.UTC))
        );
    }

    public boolean insertUsageEvent(
            UUID eventId,
            UUID emailId,
            String userId,
            long bytesDelta,
            String eventType,
            Instant processedAt
    ) {
        String sql = """
                INSERT INTO storage_usage_events (event_id, email_id, user_id, bytes_delta, event_type, processed_at)
                VALUES (:eventId, :emailId, :userId, :bytesDelta, :eventType, :processedAt)
                """;

        try {
            jdbcTemplate.update(
                    sql,
                    new MapSqlParameterSource()
                            .addValue("eventId", eventId)
                            .addValue("emailId", emailId)
                            .addValue("userId", userId)
                            .addValue("bytesDelta", bytesDelta)
                            .addValue("eventType", eventType)
                            .addValue("processedAt", OffsetDateTime.ofInstant(processedAt, java.time.ZoneOffset.UTC))
            );
            return true;
        } catch (DuplicateKeyException exception) {
            return false;
        }
    }

    private Instant toInstant(ResultSet rs, String columnName) throws SQLException {
        return rs.getObject(columnName, OffsetDateTime.class).toInstant();
    }
}
