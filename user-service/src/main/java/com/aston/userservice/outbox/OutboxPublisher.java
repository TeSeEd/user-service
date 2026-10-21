package com.aston.userservice.outbox;

import com.aston.userservice.kafka.UserEventProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OutboxPublisher {

    private static final Logger log =
            LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxClaimService claimService;
    private final OutboxCompletionService completionService;
    private final OutboxRecoveryService recoveryService;
    private final UserEventProducer producer;

    public OutboxPublisher(
            OutboxClaimService claimService,
            OutboxCompletionService completionService,
            UserEventProducer producer,
            OutboxRecoveryService recoveryService
    ) {
        this.claimService = claimService;
        this.completionService = completionService;
        this.recoveryService = recoveryService;
        this.producer = producer;
    }

    @Scheduled(fixedDelayString = "${outbox.publisher.delay-ms:5000}")
    public void publishPendingEvents() {
        List<ClaimedOutboxEvent> events = claimService.claimBatch(20);

        for (ClaimedOutboxEvent event : events) {
            try {
                producer.publish(event.userEvent()).join();

                completionService.markPublished(
                        event.eventId(),
                        event.claimToken()
                );

            } catch (Exception exception) {
                log.error(
                        "Failed to publish outbox event {}",
                        event.eventId(),
                        exception
                );

                try {
                    completionService.markFailed(
                            event.eventId(),
                            event.claimToken(),
                            exception.toString()
                    );
                } catch (Exception completionException) {
                    log.error(
                            "Failed to update outbox event {}",
                            event.eventId(),
                            completionException
                    );
                }
            }
        }
    }

    @Scheduled(fixedDelayString = "${outbox.recovery.delay-ms:60000}")
    public void recoverStaleEvents() {
        int recovered = recoveryService.recoverStaleEvents();

        if (recovered > 0) {
            log.warn("Recovered {} stale outbox events", recovered);
        }
    }
}