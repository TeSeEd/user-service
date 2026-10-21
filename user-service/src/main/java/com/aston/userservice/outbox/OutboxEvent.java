package com.aston.userservice.outbox;

import com.aston.contracts.event.UserEvent;
import com.aston.contracts.event.UserEventType;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private UserEventType eventType;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "schema_version", nullable = false)
    private Integer schemaVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OutboxStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_retry_at")
    private OffsetDateTime nextRetryAt;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Column(name = "processing_started_at")
    private OffsetDateTime processingStartedAt;

    @Column(name = "claim_token")
    private UUID claimToken;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    protected OutboxEvent() {
    }

    public OutboxEvent(UserEventType userEventType, Long userId, String email) {
        if (Objects.isNull(userId) || userId <= 0)
            throw new IllegalArgumentException("userId must be greater than 0 and not null");
        if (Objects.isNull(email) || email.isBlank())
            throw new IllegalArgumentException("email must not be null or empty");
        Objects.requireNonNull(userEventType, "eventType cannot be null");

        this.id = UUID.randomUUID();
        this.eventType = userEventType;
        this.userId = userId;
        this.email = email;
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        this.schemaVersion = UserEvent.CURRENT_SCHEMA_VERSION;
        this.status = OutboxStatus.PENDING;
        this.attemptCount = 0;
        this.nextRetryAt = null;
        this.publishedAt = null;
        this.processingStartedAt = null;
        this.claimToken = null;
        this.lastError = null;
    }

    public void markProcessing(OffsetDateTime now) {
        Objects.requireNonNull(now, "now cannot be null");

        if (this.status == OutboxStatus.FAILED
                && (this.nextRetryAt == null || this.nextRetryAt.isAfter(now))) {
            throw new IllegalStateException("Retry is not allowed yet");
        }

        if (this.status != OutboxStatus.PENDING
                && this.status != OutboxStatus.FAILED) {
            throw new IllegalStateException("Cannot mark processing when status is " + this.status);
        }

        this.status = OutboxStatus.PROCESSING;
        this.processingStartedAt = now;
        this.attemptCount++;
        this.claimToken = UUID.randomUUID();
        this.nextRetryAt = null;
    }

    public void markPublished(UUID expectedClaimToken) {
        if (this.status != OutboxStatus.PROCESSING) {
            throw new IllegalStateException("Cannot mark published when status is " + this.status);
        }

        if (this.claimToken == null
                || !this.claimToken.equals(expectedClaimToken)) {
            throw new IllegalStateException("Invalid claim token");
        }

        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = OffsetDateTime.now(ZoneOffset.UTC);
        this.processingStartedAt = null;
        this.nextRetryAt = null;
        this.lastError = null;
        this.claimToken = null;
    }

    public void markFailed(
            UUID expectedClaimToken,
            String error,
            OffsetDateTime nextRetryAt
    ) {
        if (this.status != OutboxStatus.PROCESSING) {
            throw new IllegalStateException("Cannot mark failed when status is " + this.status);
        }

        if (this.claimToken == null
                || !this.claimToken.equals(expectedClaimToken)) {
            throw new IllegalStateException("Invalid claim token");
        }

        if (error == null || error.isBlank()) {
            throw new IllegalArgumentException("error must not be null or blank");
        }

        if (nextRetryAt == null
                || !nextRetryAt.isAfter(OffsetDateTime.now(ZoneOffset.UTC))) {
            throw new IllegalArgumentException("nextRetryAt must be in the future");
        }

        this.status = OutboxStatus.FAILED;
        this.processingStartedAt = null;
        this.lastError = error;
        this.nextRetryAt = nextRetryAt;
        this.claimToken = null;
    }

    public void recoverStaleProcessing(
            OffsetDateTime now,
            OffsetDateTime staleBefore
    ) {
        Objects.requireNonNull(now, "now cannot be null");
        Objects.requireNonNull(staleBefore, "staleBefore cannot be null");

        if (this.status != OutboxStatus.PROCESSING
                || this.processingStartedAt == null
                || this.processingStartedAt.isAfter(staleBefore)) {
            throw new IllegalStateException(
                    "Outbox event is not eligible for recovery"
            );
        }

        this.status = OutboxStatus.FAILED;
        this.nextRetryAt = now;
        this.processingStartedAt = null;
        this.claimToken = null;
        this.lastError = "Processing timed out";
    }

    public UUID getId() {
        return id;
    }

    public UserEventType getEventType() {
        return eventType;
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public Integer getSchemaVersion() {
        return schemaVersion;
    }

    public OutboxStatus getStatus() {
        return status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public OffsetDateTime getNextRetryAt() {
        return nextRetryAt;
    }

    public OffsetDateTime getPublishedAt() {
        return publishedAt;
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
