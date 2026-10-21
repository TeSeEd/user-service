package com.aston.notificationservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NotificationSender {

    private static final Logger log =
            LoggerFactory.getLogger(NotificationSender.class);

    private final NotificationQueueService queueService;
    private final EmailService emailService;

    public NotificationSender(
            NotificationQueueService queueService,
            EmailService emailService
    ) {
        this.queueService = queueService;
        this.emailService = emailService;
    }

    @Scheduled(fixedDelayString = "${notification.sender.delay-ms:5000}")
    public void sendPendingNotifications() {
        List<ClaimedNotification> batch = queueService.claimBatch(20);

        for (ClaimedNotification notification : batch) {
            try {
                emailService.sendUserNotification(
                        notification.recipientEmail(),
                        notification.eventType()
                );

                boolean completed = queueService.markSent(
                        notification.id(),
                        notification.claimToken()
                );

                if (!completed) {
                    log.warn(
                            "Outdated notification claim: {}",
                            notification.id()
                    );
                }

            } catch (Exception exception) {
                log.error(
                        "Failed to send notification {}",
                        notification.id(),
                        exception
                );

                try {
                    queueService.markFailed(
                            notification.id(),
                            notification.claimToken(),
                            exception.toString()
                    );
                } catch (Exception dbException) {
                    log.error(
                            "Failed to update notification {}",
                            notification.id(),
                            dbException
                    );
                }
            }
        }
    }

    @Scheduled(fixedDelayString = "${notification.recovery.delay-ms:60000}")
    public void recoverStaleNotifications() {
        int recovered = queueService.recoverStale();

        if (recovered > 0) {
            log.warn("Recovered {} notifications", recovered);
        }
    }
}