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

    private Instant toInstant(ResultSet rs, String columnName) throws SQLException {
        return rs.getObject(columnName, OffsetDateTime.class).toInstant();
    }
}
