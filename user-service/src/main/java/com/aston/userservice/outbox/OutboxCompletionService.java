package com.aston.userservice.outbox;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class OutboxCompletionService {

    private final OutboxEventRepository repository;

    public OutboxCompletionService(OutboxEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public boolean markPublished(UUID eventId, UUID claimToken) {
        OutboxEvent event = repository.findByIdForUpdate(eventId)
                .orElseThrow(() -> new IllegalStateException(
                        "Outbox event not found: " + eventId
                ));

        if (event.getStatus() != OutboxStatus.PROCESSING
                || !claimToken.equals(event.getClaimToken())) {
            return false;
        }

        event.markPublished(claimToken);
        return true;
    }

    @Transactional
    public boolean markFailed(UUID eventId, UUID claimToken, String error) {
        OutboxEvent event = repository.findByIdForUpdate(eventId)
                .orElseThrow(() -> new IllegalStateException(
                        "Outbox event not found: " + eventId
                ));

        if (event.getStatus() != OutboxStatus.PROCESSING
                || !claimToken.equals(event.getClaimToken())) {
            return false;
        }

        OffsetDateTime retryAt =
                OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(30);

        event.markFailed(claimToken, error, retryAt);
        return true;
    }
}