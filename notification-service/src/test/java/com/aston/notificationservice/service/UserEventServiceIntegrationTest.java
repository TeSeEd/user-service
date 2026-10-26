package com.aston.notificationservice.service;

import com.aston.contracts.event.UserEvent;
import com.aston.contracts.event.UserEventType;
import com.aston.notificationservice.entity.Notification;
import com.aston.notificationservice.model.NotificationStatus;
import com.aston.notificationservice.repository.NotificationRepository;
import com.aston.notificationservice.repository.ProcessedEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {"spring.task.scheduling.enabled=false"})
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class UserEventServiceIntegrationTest {
    @Container
    @ServiceConnection
    private static final PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:17");

    @Autowired
    private UserEventService userEventService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private NotificationSender notificationSender;

    @Test
    void shouldCreateNotificationForNewEvent() {
        UUID eventId = UUID.randomUUID();

        UserEvent userEvent = new UserEvent(
                eventId,
                UserEventType.USER_CREATED,
                1L,
                "test@example.com",
                OffsetDateTime.now(ZoneOffset.UTC),
                UserEvent.CURRENT_SCHEMA_VERSION
        );

        userEventService.process(userEvent);

        Notification notification = notificationRepository
                .findByEventId(eventId)
                .orElseThrow();

        assertEquals(eventId, notification.getEventId());
        assertEquals("test@example.com", notification.getRecipientEmail());
        assertEquals(NotificationStatus.PENDING, notification.getStatus());
    }

    @Test
    void shouldNotCreateDuplicateNotification() {
        UUID eventId = UUID.randomUUID();
        UserEvent userEvent = new UserEvent(
                eventId,
                UserEventType.USER_CREATED,
                1L,
                "test@example.com",
                OffsetDateTime.now(ZoneOffset.UTC),
                UserEvent.CURRENT_SCHEMA_VERSION
        );

        userEventService.process(userEvent);
        userEventService.process(userEvent);

        assertTrue(notificationRepository.existsByEventId(eventId));

        long count = notificationRepository.findAll()
                .stream()
                .filter(n -> n.getEventId().equals(eventId))
                .count();

        assertEquals(1, count);
    }

    @Test
    void shouldRollbackEventRegistrationWhenNotificationCreationFails() {
        jdbcTemplate.execute("""
                ALTER TABLE notifications
                ADD CONSTRAINT chk_test_reject_email
                CHECK (recipient_email <> 'rollback-test@example.com')
                """);

        try {
            UUID eventId = UUID.randomUUID();
            UserEvent userEvent = new UserEvent(
                    eventId,
                    UserEventType.USER_CREATED,
                    1L,
                    "rollback-test@example.com",
                    OffsetDateTime.now(ZoneOffset.UTC),
                    UserEvent.CURRENT_SCHEMA_VERSION
            );

            assertThrows(DataIntegrityViolationException.class,
                    () -> userEventService.process(userEvent)
            );

            assertFalse(processedEventRepository.existsByEventId(eventId));
            assertFalse(notificationRepository.existsByEventId(eventId));

        } finally {
            jdbcTemplate.execute("""
                    ALTER TABLE notifications
                    DROP CONSTRAINT chk_test_reject_email
                    """);
        }
    }
}
