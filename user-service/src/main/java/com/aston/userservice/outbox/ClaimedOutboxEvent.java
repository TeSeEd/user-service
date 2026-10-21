package com.aston.userservice.outbox;

import com.aston.contracts.event.UserEvent;

import java.util.UUID;

public record ClaimedOutboxEvent(
        UUID eventId,
        UUID claimToken,
        UserEvent userEvent
) {
}