
package com.aston.userservice.outbox;

import com.aston.contracts.event.UserEvent;
import com.aston.contracts.event.UserEventType;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OutboxEventTest {

    private OutboxEvent createEvent() {
        return new OutboxEvent(
                UserEventType.USER_CREATED,
                1L,
                "test@example.com"
        );
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    @Test
    void constructor_shouldInitializePendingEvent() {
        OutboxEvent event = createEvent();

        assertNotNull(event.getId());
        assertEquals(UserEventType.USER_CREATED, event.getEventType());
        assertEquals(1L, event.getUserId());
        assertEquals("test@example.com", event.getEmail());

        assertNotNull(event.getCreatedAt());
        assertEquals(
                UserEvent.CURRENT_SCHEMA_VERSION,
                event.getSchemaVersion()
        );

        assertEquals(OutboxStatus.PENDING, event.getStatus());
        assertEquals(0, event.getAttemptCount());

        assertNull(event.getNextRetryAt());
        assertNull(event.getPublishedAt());
        assertNull(event.getProcessingStartedAt());
        assertNull(event.getClaimToken());
        assertNull(event.getLastError());
    }

    @Test
    void constructor_shouldSupportUserDeletedEvent() {
        OutboxEvent event = new OutboxEvent(
                UserEventType.USER_DELETED,
                2L,
                "deleted@example.com"
        );

        assertEquals(UserEventType.USER_DELETED, event.getEventType());
        assertEquals(2L, event.getUserId());
        assertEquals("deleted@example.com", event.getEmail());
        assertEquals(OutboxStatus.PENDING, event.getStatus());
    }

    @Test
    void constructor_shouldGenerateUniqueEventIds() {
        OutboxEvent first = createEvent();
        OutboxEvent second = createEvent();

        assertNotEquals(first.getId(), second.getId());
    }

    @Test
    void constructor_shouldThrowException_whenUserIdIsNull() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new OutboxEvent(
                        UserEventType.USER_CREATED,
                        null,
                        "test@example.com"
                )
        );
    }

    @Test
    void constructor_shouldThrowException_whenUserIdIsNotPositive() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new OutboxEvent(
                        UserEventType.USER_CREATED,
                        0L,
                        "test@example.com"
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new OutboxEvent(
                        UserEventType.USER_CREATED,
                        -1L,
                        "test@example.com"
                )
        );
    }

    @Test
    void constructor_shouldThrowException_whenEmailIsInvalid() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new OutboxEvent(
                        UserEventType.USER_CREATED,
                        1L,
                        null
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new OutboxEvent(
                        UserEventType.USER_CREATED,
                        1L,
                        ""
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new OutboxEvent(
                        UserEventType.USER_CREATED,
                        1L,
                        "   "
                )
        );
    }

    @Test
    void constructor_shouldThrowException_whenEventTypeIsNull() {
        assertThrows(
                NullPointerException.class,
                () -> new OutboxEvent(
                        null,
                        1L,
                        "test@example.com"
                )
        );
    }

    @Test
    void markProcessing_shouldChangeStatusAndIncrementAttemptCount() {
        OutboxEvent event = createEvent();
        OffsetDateTime now = now();

        event.markProcessing(now);

        assertEquals(OutboxStatus.PROCESSING, event.getStatus());
        assertEquals(1, event.getAttemptCount());
        assertEquals(now, event.getProcessingStartedAt());

        assertNotNull(event.getClaimToken());
        assertNull(event.getNextRetryAt());
        assertNull(event.getPublishedAt());
    }

    @Test
    void markProcessing_shouldThrowException_whenAlreadyProcessing() {
        OutboxEvent event = createEvent();
        OffsetDateTime now = now();

        event.markProcessing(now);
        UUID claimToken = event.getClaimToken();

        assertThrows(
                IllegalStateException.class,
                () -> event.markProcessing(now)
        );

        assertEquals(OutboxStatus.PROCESSING, event.getStatus());
        assertEquals(1, event.getAttemptCount());
        assertEquals(now, event.getProcessingStartedAt());
        assertEquals(claimToken, event.getClaimToken());
    }

    @Test
    void markProcessing_shouldThrowException_whenNowIsNull() {
        OutboxEvent event = createEvent();

        assertThrows(
                NullPointerException.class,
                () -> event.markProcessing(null)
        );

        assertEquals(OutboxStatus.PENDING, event.getStatus());
        assertEquals(0, event.getAttemptCount());
        assertNull(event.getClaimToken());
    }

    @Test
    void markPublished_shouldChangeStatusAndSetPublishedAt() {
        OutboxEvent event = createEvent();

        event.markProcessing(now());
        UUID claimToken = event.getClaimToken();

        event.markPublished(claimToken);

        assertEquals(OutboxStatus.PUBLISHED, event.getStatus());
        assertNotNull(event.getPublishedAt());

        assertNull(event.getProcessingStartedAt());
        assertNull(event.getNextRetryAt());
        assertNull(event.getLastError());
        assertNull(event.getClaimToken());

        assertEquals(1, event.getAttemptCount());
    }

    @Test
    void markPublished_shouldThrowException_whenEventIsPending() {
        OutboxEvent event = createEvent();

        assertThrows(
                IllegalStateException.class,
                () -> event.markPublished(UUID.randomUUID())
        );

        assertEquals(OutboxStatus.PENDING, event.getStatus());
        assertNull(event.getPublishedAt());
        assertNull(event.getClaimToken());
    }

    @Test
    void markPublished_shouldThrowException_whenAlreadyPublished() {
        OutboxEvent event = createEvent();

        event.markProcessing(now());
        UUID claimToken = event.getClaimToken();

        event.markPublished(claimToken);

        OffsetDateTime publishedAt = event.getPublishedAt();

        assertThrows(
                IllegalStateException.class,
                () -> event.markPublished(claimToken)
        );

        assertEquals(OutboxStatus.PUBLISHED, event.getStatus());
        assertEquals(publishedAt, event.getPublishedAt());
        assertEquals(1, event.getAttemptCount());
        assertNull(event.getClaimToken());
    }

    @Test
    void markPublished_shouldThrowException_whenClaimTokenIsInvalid() {
        OutboxEvent event = createEvent();

        event.markProcessing(now());
        UUID correctToken = event.getClaimToken();
        UUID invalidToken = UUID.randomUUID();

        assertThrows(
                IllegalStateException.class,
                () -> event.markPublished(invalidToken)
        );

        assertThrows(
                IllegalStateException.class,
                () -> event.markPublished(null)
        );

        assertEquals(OutboxStatus.PROCESSING, event.getStatus());
        assertEquals(correctToken, event.getClaimToken());
        assertNull(event.getPublishedAt());
        assertEquals(1, event.getAttemptCount());
    }

    @Test
    void markFailed_shouldSaveErrorAndScheduleRetry() {
        OutboxEvent event = createEvent();

        OffsetDateTime now = now();
        OffsetDateTime retryAt = now.plusMinutes(5);

        event.markProcessing(now);
        UUID claimToken = event.getClaimToken();

        event.markFailed(
                claimToken,
                "Kafka is unavailable",
                retryAt
        );

        assertEquals(OutboxStatus.FAILED, event.getStatus());
        assertEquals("Kafka is unavailable", event.getLastError());
        assertEquals(retryAt, event.getNextRetryAt());

        assertNull(event.getProcessingStartedAt());
        assertNull(event.getPublishedAt());
        assertNull(event.getClaimToken());

        assertEquals(1, event.getAttemptCount());
    }

    @Test
    void markFailed_shouldThrowException_whenEventIsPending() {
        OutboxEvent event = createEvent();

        assertThrows(
                IllegalStateException.class,
                () -> event.markFailed(
                        UUID.randomUUID(),
                        "Kafka is unavailable",
                        now().plusMinutes(5)
                )
        );

        assertEquals(OutboxStatus.PENDING, event.getStatus());
        assertEquals(0, event.getAttemptCount());
        assertNull(event.getLastError());
        assertNull(event.getNextRetryAt());
        assertNull(event.getClaimToken());
    }

    @Test
    void markFailed_shouldThrowException_whenClaimTokenIsInvalid() {
        OutboxEvent event = createEvent();

        OffsetDateTime now = now();
        event.markProcessing(now);

        UUID correctToken = event.getClaimToken();
        UUID invalidToken = UUID.randomUUID();
        OffsetDateTime retryAt = now.plusMinutes(5);

        assertThrows(
                IllegalStateException.class,
                () -> event.markFailed(
                        invalidToken,
                        "Kafka error",
                        retryAt
                )
        );

        assertThrows(
                IllegalStateException.class,
                () -> event.markFailed(
                        null,
                        "Kafka error",
                        retryAt
                )
        );

        assertEquals(OutboxStatus.PROCESSING, event.getStatus());
        assertEquals(correctToken, event.getClaimToken());
        assertNull(event.getNextRetryAt());
        assertNull(event.getLastError());
    }

    @Test
    void markFailed_shouldThrowException_whenErrorIsInvalid() {
        OutboxEvent event = createEvent();

        OffsetDateTime now = now();
        event.markProcessing(now);

        UUID claimToken = event.getClaimToken();
        OffsetDateTime retryAt = now.plusMinutes(5);

        assertThrows(
                IllegalArgumentException.class,
                () -> event.markFailed(
                        claimToken,
                        null,
                        retryAt
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> event.markFailed(
                        claimToken,
                        "",
                        retryAt
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> event.markFailed(
                        claimToken,
                        "   ",
                        retryAt
                )
        );

        assertEquals(OutboxStatus.PROCESSING, event.getStatus());
        assertEquals(1, event.getAttemptCount());
        assertEquals(now, event.getProcessingStartedAt());
        assertEquals(claimToken, event.getClaimToken());
    }

    @Test
    void markFailed_shouldThrowException_whenRetryTimeIsInvalid() {
        OutboxEvent event = createEvent();

        OffsetDateTime now = now();
        event.markProcessing(now);

        UUID claimToken = event.getClaimToken();

        assertThrows(
                IllegalArgumentException.class,
                () -> event.markFailed(
                        claimToken,
                        "Kafka error",
                        null
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> event.markFailed(
                        claimToken,
                        "Kafka error",
                        now.minusMinutes(5)
                )
        );

        assertEquals(OutboxStatus.PROCESSING, event.getStatus());
        assertEquals(1, event.getAttemptCount());
        assertNull(event.getNextRetryAt());
        assertEquals(claimToken, event.getClaimToken());
    }

    @Test
    void markProcessing_shouldThrowException_whenRetryTimeHasNotCome() {
        OutboxEvent event = createEvent();

        OffsetDateTime now = now();
        OffsetDateTime retryAt = now.plusMinutes(5);

        event.markProcessing(now);
        UUID claimToken = event.getClaimToken();

        event.markFailed(
                claimToken,
                "Kafka is unavailable",
                retryAt
        );

        assertThrows(
                IllegalStateException.class,
                () -> event.markProcessing(now.plusMinutes(2))
        );

        assertEquals(OutboxStatus.FAILED, event.getStatus());
        assertEquals(1, event.getAttemptCount());
        assertEquals(retryAt, event.getNextRetryAt());
        assertEquals("Kafka is unavailable", event.getLastError());
        assertNull(event.getClaimToken());
    }

    @Test
    void markProcessing_shouldRetry_whenRetryTimeHasCome() {
        OutboxEvent event = createEvent();

        OffsetDateTime now = now();
        OffsetDateTime retryAt = now.plusMinutes(5);

        event.markProcessing(now);
        UUID firstToken = event.getClaimToken();

        event.markFailed(
                firstToken,
                "Kafka is unavailable",
                retryAt
        );

        event.markProcessing(retryAt);

        assertEquals(OutboxStatus.PROCESSING, event.getStatus());
        assertEquals(2, event.getAttemptCount());
        assertEquals(retryAt, event.getProcessingStartedAt());
        assertNull(event.getNextRetryAt());

        assertNotNull(event.getClaimToken());
        assertNotEquals(firstToken, event.getClaimToken());
    }

    @Test
    void retry_shouldPreserveEventIdAndAllowSuccessfulPublication() {
        OutboxEvent event = createEvent();

        UUID originalEventId = event.getId();

        OffsetDateTime now = now();
        OffsetDateTime retryAt = now.plusMinutes(5);

        event.markProcessing(now);
        UUID firstToken = event.getClaimToken();

        event.markFailed(
                firstToken,
                "Kafka is unavailable",
                retryAt
        );

        event.markProcessing(retryAt);
        UUID secondToken = event.getClaimToken();

        event.markPublished(secondToken);

        assertEquals(originalEventId, event.getId());
        assertNotEquals(firstToken, secondToken);

        assertEquals(OutboxStatus.PUBLISHED, event.getStatus());
        assertEquals(2, event.getAttemptCount());

        assertNotNull(event.getPublishedAt());
        assertNull(event.getProcessingStartedAt());
        assertNull(event.getNextRetryAt());
        assertNull(event.getLastError());
        assertNull(event.getClaimToken());
    }

    @Test
    void retry_shouldRejectOldClaimToken() {
        OutboxEvent event = createEvent();

        OffsetDateTime now = now();
        OffsetDateTime retryAt = now.plusMinutes(5);

        event.markProcessing(now);
        UUID firstToken = event.getClaimToken();

        event.markFailed(
                firstToken,
                "Kafka is unavailable",
                retryAt
        );

        event.markProcessing(retryAt);
        UUID secondToken = event.getClaimToken();

        assertNotEquals(firstToken, secondToken);

        assertThrows(
                IllegalStateException.class,
                () -> event.markPublished(firstToken)
        );

        assertThrows(
                IllegalStateException.class,
                () -> event.markFailed(
                        firstToken,
                        "Another failure",
                        retryAt.plusMinutes(5)
                )
        );

        assertEquals(OutboxStatus.PROCESSING, event.getStatus());
        assertEquals(secondToken, event.getClaimToken());
        assertEquals(2, event.getAttemptCount());
        assertNull(event.getPublishedAt());
    }

    @Test
    void retry_shouldAllowOnlyCurrentClaimTokenToComplete() {
        OutboxEvent event = createEvent();

        OffsetDateTime now = now();
        OffsetDateTime retryAt = now.plusMinutes(5);

        event.markProcessing(now);
        UUID firstToken = event.getClaimToken();

        event.markFailed(
                firstToken,
                "Kafka is unavailable",
                retryAt
        );

        event.markProcessing(retryAt);
        UUID secondToken = event.getClaimToken();

        assertThrows(
                IllegalStateException.class,
                () -> event.markPublished(firstToken)
        );

        event.markPublished(secondToken);

        assertEquals(OutboxStatus.PUBLISHED, event.getStatus());
        assertEquals(2, event.getAttemptCount());
        assertNotNull(event.getPublishedAt());
        assertNull(event.getClaimToken());
    }
}
