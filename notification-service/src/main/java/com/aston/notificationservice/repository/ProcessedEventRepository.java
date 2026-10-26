package com.aston.notificationservice.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class ProcessedEventRepository {
    private final JdbcTemplate jdbcTemplate;


    public ProcessedEventRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean existsByEventId(UUID eventId) {
        return Boolean.TRUE.equals(
                jdbcTemplate.queryForObject("""
                        SELECT EXISTS (
                        SELECT 1
                        FROM processed_user_events
                        WHERE event_id = ?
                        )""", Boolean.class, eventId)
        );
    }

    public boolean registerIfAbsent(UUID eventId) {
        return 0 < jdbcTemplate.update("""
                INSERT INTO processed_user_events (event_id)
                VALUES (?)
                ON CONFLICT (event_id) DO NOTHING
                """, eventId);
    }
}
