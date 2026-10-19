package com.aston.notificationservice.repository;

import com.aston.notificationservice.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Optional<Notification> findByEventId(UUID eventId);

    boolean existsByEventId(UUID eventId);
}
