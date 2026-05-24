package com.mailvault.ingestion.outbox;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Repository
public class OutboxEventRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public OutboxEventRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(OutboxEvent event) {
        String sql = """
                INSERT INTO outbox_events (id, aggregate_id, event_type, payload, published, created_at)
                VALUES (:id, :aggregateId, :eventType, CAST(:payload AS jsonb), FALSE, :createdAt)
                """;

        jdbcTemplate.update(
                sql,
                new MapSqlParameterSource()
                        .addValue("id", event.id())
                        .addValue("aggregateId", event.aggregateId())
                        .addValue("eventType", event.eventType())
                        .addValue("payload", event.payload())
                        .addValue("createdAt", OffsetDateTime.ofInstant(event.createdAt(), ZoneOffset.UTC))
        );
    }

    public List<OutboxEvent> findUnpublished(int limit) {
        String sql = """
                SELECT id, aggregate_id, event_type, payload::text AS payload, created_at
                  FROM outbox_events
                 WHERE published = FALSE
                 ORDER BY created_at
                 LIMIT :limit
                """;

        return jdbcTemplate.query(
                sql,
                new MapSqlParameterSource("limit", limit),
                (rs, rowNum) -> toEvent(rs)
        );
    }

    public void markPublished(UUID eventId) {
        jdbcTemplate.update(
                "UPDATE outbox_events SET published = TRUE WHERE id = :eventId",
                new MapSqlParameterSource("eventId", eventId)
        );
    }

    private OutboxEvent toEvent(ResultSet rs) throws SQLException {
        return new OutboxEvent(
                rs.getObject("id", UUID.class),
                rs.getObject("aggregate_id", UUID.class),
                rs.getString("event_type"),
                rs.getString("payload"),
                rs.getObject("created_at", OffsetDateTime.class).toInstant()
        );
    }
}
