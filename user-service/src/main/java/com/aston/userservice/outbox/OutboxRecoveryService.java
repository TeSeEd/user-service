package com.aston.userservice.outbox;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class OutboxRecoveryService {

    private final OutboxEventRepository repository;

    public OutboxRecoveryService(OutboxEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public int recoverStaleEvents() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime staleBefore = now.minusMinutes(5);

        List<OutboxEvent> events =
                repository.findStaleProcessing(staleBefore, 100);

        for (OutboxEvent event : events) {
            event.recoverStaleProcessing(now, staleBefore);
        }

        return events.size();
    }
}