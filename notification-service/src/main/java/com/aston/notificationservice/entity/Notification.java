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

    @Column(name = "event_type", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private EventType eventType;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private NotificationStatus status;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "sent_at")
    private OffsetDateTime sentAt;

    @Column(name = "next_retry_at")
    private OffsetDateTime nextRetryAt;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    protected Notification() {

    }

    public Notification(UUID eventId, String recipientEmail, EventType eventType) {
        Objects.requireNonNull(eventId, "eventId cannot be null");
        Objects.requireNonNull(recipientEmail, "recipientEmail cannot be null");
        Objects.requireNonNull(eventType, "eventType cannot be null");
        this.eventId = eventId;
        this.recipientEmail = recipientEmail;
        this.eventType = eventType;
        this.status = NotificationStatus.PENDING;
        this.attemptCount = 0;
    }

    public Long getId() {
        return this.id;
    }

    public UUID getEventId() {
        return this.eventId;
    }

    public String getRecipientEmail() {
        return this.recipientEmail;
    }

    public EventType getEventType() {
        return this.eventType;
    }

    public NotificationStatus getStatus() {
        return this.status;
    }

    public Integer getAttemptCount() {
        return this.attemptCount;
    }

    public OffsetDateTime getCreatedAt() {
        return this.createdAt;
    }

    public OffsetDateTime getSentAt() {
        return this.sentAt;
    }

    public OffsetDateTime getNextRetryAt() {
        return this.nextRetryAt;
    }

    public String getLastError() {
        return this.lastError;
    }

    @PrePersist
    private void onCreate() {
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
