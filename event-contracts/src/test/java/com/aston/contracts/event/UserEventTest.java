package com.aston.contracts.event;


import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;


class UserEventTest {
    @Test
    void shouldCreateValidUserEvent() {
        UserEvent userEvent = new UserEventBuilder().build();

        assertNotNull(userEvent.eventId());
        assertEquals(UserEventType.USER_CREATED, userEvent.eventType());
        assertEquals(1L, userEvent.userId());
        assertEquals("example@mail.com", userEvent.email());
        assertNotNull(userEvent.occurredAt());
        assertEquals(UserEvent.CURRENT_SCHEMA_VERSION, userEvent.schemaVersion());

    }

    @Test
    void shouldRejectNullEventId() {
        assertThrows(NullPointerException.class, () ->
                new UserEventBuilder().withEventId(null).build());
    }

    @Test
    void shouldRejectNonPositiveUserId() {
        assertThrows(IllegalArgumentException.class, () ->
                new UserEventBuilder().withUserId(-1L).build());
    }

    @Test
    void shouldRejectZeroUserId() {
        assertThrows(IllegalArgumentException.class, () ->
                new UserEventBuilder().withUserId(0L).build());
    }

    @Test
    void shouldRejectBlankEmail() {
        assertThrows(IllegalArgumentException.class, () ->
                new UserEventBuilder().withEmail("       ").build());
    }


    private static class UserEventBuilder {
        private UUID eventId = UUID.randomUUID();
        private UserEventType eventType = UserEventType.USER_CREATED;
        private Long userId = 1L;
        private String email = "example@mail.com";
        private OffsetDateTime occurredAt = OffsetDateTime.now(ZoneOffset.UTC);
        private int schemaVersion = UserEvent.CURRENT_SCHEMA_VERSION;

        private UserEventBuilder withEventId(UUID eventId) {
            this.eventId = eventId;
            return this;
        }

        private UserEventBuilder withEventType(UserEventType eventType) {
            this.eventType = eventType;
            return this;
        }

        private UserEventBuilder withUserId(Long userId) {
            this.userId = userId;
            return this;
        }

        private UserEventBuilder withEmail(String email) {
            this.email = email;
            return this;
        }

        public UserEvent build() {
            return new UserEvent(
                    eventId,
                    eventType,
                    userId,
                    email,
                    occurredAt,
                    schemaVersion
            );
        }
    }
}
