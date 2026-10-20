package com.aston.contracts.event;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

public record UserEvent(
        UUID eventId,
        UserEventType eventType,
        Long userId,
        String email,
        OffsetDateTime occurredAt,
        Integer schemaVersion
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public UserEvent {
        Objects.requireNonNull(eventId, "eventId cannot be null");
        Objects.requireNonNull(eventType, "eventType cannot be null");

        if (Objects.isNull(userId) || userId <= 0)
            throw new IllegalArgumentException("userId must be positive and cannot be null");

        if (Objects.isNull(email) || email.isBlank())
            throw new IllegalArgumentException("email cannot be null or blank");

        Objects.requireNonNull(occurredAt, "occurredAt cannot be null");

        if (Objects.isNull(schemaVersion) || schemaVersion != CURRENT_SCHEMA_VERSION)
            throw new IllegalArgumentException("Unsupported schema version");
    }
}
