package com.aston.notificationservice.service;

import com.aston.notificationservice.entity.Notification;
import com.aston.notificationservice.model.NotificationStatus;
import com.aston.notificationservice.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationQueueService {

    private final NotificationRepository repository;

    public NotificationQueueService(NotificationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public List<ClaimedNotification> claimBatch(int batchSize) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        return repository.findReady(now, batchSize)
                .stream()
                .map(notification -> {
                    notification.markProcessing();

                    return new ClaimedNotification(
                            notification.getId(),
                            notification.getClaimToken(),
                            notification.getRecipientEmail(),
                            notification.getEventType()
                    );
                })
                .toList();
    }

    @Transactional
    public boolean markSent(Long id, UUID claimToken) {
        Notification notification = repository.findByIdForUpdate(id)
                .orElseThrow();

        if (notification.getStatus() != NotificationStatus.PROCESSING
                || !claimToken.equals(notification.getClaimToken())) {
            return false;
        }

        notification.markSent();
        return true;
    }

    @Transactional
    public boolean markFailed(
            Long id,
            UUID claimToken,
            String error
    ) {
        Notification notification = repository.findByIdForUpdate(id)
                .orElseThrow();

        if (notification.getStatus() != NotificationStatus.PROCESSING
                || !claimToken.equals(notification.getClaimToken())) {
            return false;
        }

        int attempts = notification.getAttemptCount();
        long delaySeconds = Math.min(30L * attempts, 300L);

        OffsetDateTime retryAt =
                OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(delaySeconds);

        notification.markFailed(error, retryAt);
        return true;
    }

    @Transactional
    public int recoverStale() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        OffsetDateTime staleBefore = now.minusMinutes(5);

        List<Notification> stale =
                repository.findStale(staleBefore, 100);

        for (Notification notification : stale) {
            notification.recoverStale(now);
        }

        return stale.size();
    }
}