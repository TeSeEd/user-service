package com.aston.notificationservice.repository;

import com.aston.notificationservice.entity.Notification;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    Optional<Notification> findByEventId(UUID eventId);

    boolean existsByEventId(UUID eventId);

    @Query(value = """
            SELECT *
            FROM notifications
            WHERE status = 'PENDING'
               OR (status = 'FAILED'
                   AND next_retry_at <= :now)
            ORDER BY created_at, id
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<Notification> findReady(
            @Param("now") OffsetDateTime now,
            @Param("batchSize") int batchSize
    );

    @Query(value = """
            SELECT *
            FROM notifications
            WHERE status = 'PROCESSING'
              AND processing_started_at <= :staleBefore
            ORDER BY processing_started_at, id
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<Notification> findStale(
            @Param("staleBefore") OffsetDateTime staleBefore,
            @Param("batchSize") int batchSize
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT n FROM Notification n WHERE n.id = :id")
    Optional<Notification> findByIdForUpdate(@Param("id") Long id);
}