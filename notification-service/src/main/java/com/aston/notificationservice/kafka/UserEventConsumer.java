package com.aston.notificationservice.kafka;

import com.aston.contracts.event.UserEvent;
import com.aston.notificationservice.model.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class UserEventConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(UserEventConsumer.class);

    private final JdbcTemplate jdbcTemplate;

    public UserEventConsumer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    @KafkaListener(
            topics = "user.events.v1",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(UserEvent event) {

        int inserted = jdbcTemplate.update("""
                INSERT INTO processed_user_events (event_id)
                VALUES (?)
                ON CONFLICT (event_id) DO NOTHING
                """, event.eventId());

        if (inserted == 0) {
            log.info("Duplicate event skipped: {}", event.eventId());
            return;
        }

        EventType type = EventType.valueOf(event.eventType().name());

        jdbcTemplate.update("""
                        INSERT INTO notifications
                            (event_id, recipient_email, event_type,
                             status, attempt_count)
                        VALUES (?, ?, ?, 'PENDING', 0)
                        """,
                event.eventId(),
                event.email(),
                type.name()
        );

        log.info(
                "Notification queued: eventId={}, type={}",
                event.eventId(),
                type
        );
    }
}