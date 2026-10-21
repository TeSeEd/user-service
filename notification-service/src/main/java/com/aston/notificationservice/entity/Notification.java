
package com.aston.notificationservice.entity;

import com.aston.notificationservice.model.EventType;
import com.aston.notificationservice.model.NotificationStatus;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(name = "recipient_email", nullable = false, length = 255)
    private String recipientEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private EventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "sent_at")
    private OffsetDateTime sentAt;

    @Column(name = "next_retry_at")
    private OffsetDateTime nextRetryAt;

    @Column(name = "processing_started_at")
    private OffsetDateTime processingStartedAt;

    @Column(name = "claim_token")
    private UUID claimToken;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    protected Notification() {
    }

    public Notification(
            UUID eventId,
            String recipientEmail,
            EventType eventType
    ) {
        this.eventId = Objects.requireNonNull(eventId);
        this.eventType = Objects.requireNonNull(eventType);

        if (recipientEmail == null || recipientEmail.isBlank()) {
            throw new IllegalArgumentException(
                    "recipientEmail must not be blank"
            );
        }

        this.recipientEmail = recipientEmail;
        this.status = NotificationStatus.PENDING;
        this.attemptCount = 0;
    }

    @PrePersist
    private void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        }
    }

    public void markProcessing() {
        if (status != NotificationStatus.PENDING
                && status != NotificationStatus.FAILED) {
            throw new IllegalStateException(
                    "Cannot process notification in status " + status
            );
        }

        status = NotificationStatus.PROCESSING;
        attemptCount++;
        nextRetryAt = null;
        processingStartedAt = OffsetDateTime.now(ZoneOffset.UTC);
        claimToken = UUID.randomUUID();
    }

    public void markSent() {
        if (status != NotificationStatus.PROCESSING) {
            throw new IllegalStateException(
                    "Notification is not processing"
            );
        }

        status = NotificationStatus.SENT;
        sentAt = OffsetDateTime.now(ZoneOffset.UTC);
        lastError = null;
        nextRetryAt = null;
        processingStartedAt = null;
        claimToken = null;
    }

    public void markFailed(String error, OffsetDateTime retryAt) {
        if (status != NotificationStatus.PROCESSING) {
            throw new IllegalStateException(
                    "Notification is not processing"
            );
        }

        if (error == null || error.isBlank()) {
            throw new IllegalArgumentException(
                    "Error must not be blank"
            );
        }

        status = NotificationStatus.FAILED;
        lastError = error;
        nextRetryAt = Objects.requireNonNull(retryAt);
        processingStartedAt = null;
        claimToken = null;
    }

    public void recoverStale(OffsetDateTime retryAt) {
        if (status != NotificationStatus.PROCESSING) {
            throw new IllegalStateException(
                    "Notification is not processing"
            );
        }

        status = NotificationStatus.FAILED;
        lastError = "Processing timeout";
        nextRetryAt = Objects.requireNonNull(retryAt);
        processingStartedAt = null;
        claimToken = null;
    }

    public Long getId() {
        return id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public EventType getEventType() {
        return eventType;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public Integer getAttemptCount() {
        return attemptCount;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getSentAt() {
        return sentAt;
    }

    public OffsetDateTime getNextRetryAt() {
        return nextRetryAt;
    }

    public OffsetDateTime getProcessingStartedAt() {
        return processingStartedAt;
    }

    public UUID getClaimToken() {
        return claimToken;
    }

    public String getLastError() {
        return lastError;
    }
}
