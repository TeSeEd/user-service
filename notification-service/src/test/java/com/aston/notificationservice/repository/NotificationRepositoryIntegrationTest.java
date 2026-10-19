package com.aston.notificationservice.repository;

import com.aston.notificationservice.entity.Notification;
import com.aston.notificationservice.model.EventType;
import com.aston.notificationservice.model.NotificationStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class NotificationRepositoryIntegrationTest {
    @Container
    @ServiceConnection
    private static final PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer("postgres:17");

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private TestEntityManager testEntityManager;

    @Test
    void save_shouldPersistNotificationWithPendingStatus() {
        Notification notification = new Notification(UUID.randomUUID(), "example@mail.com", EventType.USER_CREATED);

        Notification savedNotification = notificationRepository.saveAndFlush(notification);

        testEntityManager.clear();

        Optional<Notification> foundNotification = notificationRepository.findByEventId(savedNotification.getEventId());

        assertNotNull(savedNotification.getId());
        assertTrue(foundNotification.isPresent());

        Notification actual = foundNotification.orElseThrow();

        assertEquals(savedNotification.getId(), actual.getId());
        assertEquals(EventType.USER_CREATED, actual.getEventType());
        assertEquals(NotificationStatus.PENDING, actual.getStatus());
        assertEquals(savedNotification.getEventId(), actual.getEventId());
        assertEquals("example@mail.com", actual.getRecipientEmail());
        assertEquals(0, actual.getAttemptCount());

        assertNotNull(actual.getCreatedAt());
        assertNull(actual.getSentAt());
        assertNull(actual.getNextRetryAt());
        assertNull(actual.getLastError());
    }

    @Test
    void save_shouldRejectDuplicateEventId() {
        UUID eventId = UUID.randomUUID();
        Notification notification1 = new Notification(eventId, "first@mail.com", EventType.USER_CREATED);
        Notification notification2 = new Notification(eventId, "second@mail.com", EventType.USER_DELETED);

        notificationRepository.saveAndFlush(notification1);
        assertThrows(DataIntegrityViolationException.class, () -> notificationRepository.saveAndFlush(notification2));
    }

    @Test
    void findByEventId_shouldReturnEmpty_whenEventDoesNotExist() {
        UUID eventId = UUID.randomUUID();
        Optional<Notification> foundNotification = notificationRepository.findByEventId(eventId);

        assertTrue(foundNotification.isEmpty());
    }

    @Test
    void existsByEventId_shouldReturnFalse_whenEventDoesNotExist() {
        UUID eventId = UUID.randomUUID();
        boolean exists = notificationRepository.existsByEventId(eventId);

        assertFalse(exists);
    }

    @Test
    void existsByEventId_shouldReturnTrue_whenEventExists() {
        UUID eventId = UUID.randomUUID();

        Notification notification = new Notification(eventId, "example@mail.com", EventType.USER_CREATED);
        notificationRepository.saveAndFlush(notification);

        assertTrue(notificationRepository.existsByEventId(eventId));
    }

    @Test
    void findByEventId_shouldReturnCorrectNotification_whenMultipleExist() {
        UUID eventId = UUID.randomUUID();

        Notification notification1 = new Notification(UUID.randomUUID(), "first@mail.com", EventType.USER_CREATED);
        Notification notification2 = new Notification(eventId, "second@mail.com", EventType.USER_CREATED);
        Notification notification3 = new Notification(UUID.randomUUID(), "third@mail.com", EventType.USER_CREATED);

        notificationRepository.saveAndFlush(notification1);
        notificationRepository.saveAndFlush(notification2);
        notificationRepository.saveAndFlush(notification3);
        testEntityManager.clear();

        Optional<Notification> foundNotification = notificationRepository.findByEventId(eventId);

        assertTrue(foundNotification.isPresent());
        assertEquals(eventId, foundNotification.get().getEventId());
        assertEquals("second@mail.com", foundNotification.get().getRecipientEmail());
        assertEquals(EventType.USER_CREATED, foundNotification.get().getEventType());
    }
}
