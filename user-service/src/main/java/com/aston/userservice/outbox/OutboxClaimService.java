package com.aston.userservice.outbox;

import com.aston.contracts.event.UserEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class OutboxClaimService {

    private final OutboxEventRepository repository;

    public OutboxClaimService(OutboxEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public List<ClaimedOutboxEvent> claimBatch(int batchSize) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize must be positive");
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        List<OutboxEvent> events =
                repository.findReadyForProcessing(now, batchSize);

        return events.stream()
                .map(event -> {
                    event.markProcessing(now);

                    UserEvent userEvent = new UserEvent(
                            event.getId(),
                            event.getEventType(),
                            event.getUserId(),
                            event.getEmail(),
                            event.getCreatedAt(),
                            event.getSchemaVersion()
                    );

                    return new ClaimedOutboxEvent(
                            event.getId(),
                            event.getClaimToken(),
                            userEvent
                    );
                })
                .toList();
    }
}