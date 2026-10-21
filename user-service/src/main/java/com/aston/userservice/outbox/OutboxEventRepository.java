package com.aston.userservice.outbox;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    @Query(value = """
            SELECT *
            FROM outbox_events
            WHERE status = 'PENDING'
               OR (status = 'FAILED' AND next_retry_at <= :now)
            ORDER BY created_at, id
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> findReadyForProcessing(
            @Param("now") OffsetDateTime now,
            @Param("batchSize") int batchSize
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM OutboxEvent e WHERE e.id = :id")
    Optional<OutboxEvent> findByIdForUpdate(@Param("id") UUID id);

    @Query(value = """
            SELECT *
            FROM outbox_events
            WHERE status = 'PROCESSING'
              AND processing_started_at <= :staleBefore
            ORDER BY processing_started_at, id
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> findStaleProcessing(
            @Param("staleBefore") OffsetDateTime staleBefore,
            @Param("batchSize") int batchSize
    );
}